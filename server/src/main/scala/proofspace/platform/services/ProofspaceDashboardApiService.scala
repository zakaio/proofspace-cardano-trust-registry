package proofspace.platform.services


import scala.concurrent.*
import cps.*
import cps.monads.{*, given}
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromString}
import com.github.rssh.appcontext.{AppContext, AppContextProvider}
import org.bouncycastle.util.io.pem.PemReader
import org.slf4j.LoggerFactory
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import proofspace.platform.model.*
import proofspace.platform.dto.*
import proofspace.platform.util.ProofspaceDashboardKey
import proofspace.trustregistry.controllers.HttpException
import proofspace.trustregistry.{AppConfig, ProofspaceNetworkConfig}
import sttp.client3.{SttpBackend, asString, basicRequest}
import sttp.client3.jsoniter.*
import sttp.model.{StatusCode, Uri}

import java.io.StringReader
import java.security.{KeyFactory, MessageDigest}
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.time.OffsetDateTime
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import scala.util.control.NonFatal


class NetworkLocalCache[T](byKey: ConcurrentHashMap[String, T] = new ConcurrentHashMap[String,T]()) {

  def find(key: String): Option[T] =
    Option(byKey.get(key))

  def update(key: String, value: T): Unit =
    val _ = byKey.put(key, value)

  def remove(key: String): Boolean = {
    byKey.remove(key) != null
  }

}

class NetworkCache[T](byNetworks: ConcurrentHashMap[String, NetworkLocalCache[T]] = new ConcurrentHashMap[String,NetworkLocalCache[T]] ) {

  def find(key: String, network: String): Option[T] = {
    Option(byNetworks.get(network)).flatMap(_.find(key))
  }

  def update(key: String, network: String, value: T): Unit = {
    val networkCache = Option(byNetworks.get(network)) match
      case Some(cache) => cache
      case None =>
        val newCache = new NetworkLocalCache[T]()
        byNetworks.put(network, newCache)
        newCache
    networkCache.update(key, value)
  }

  def remove(key: String, network: String): Boolean = {
    Option(byNetworks.get(network)) match
      case Some(cache) => cache.remove(key)
      case None => false
  }

}



class ProofspaceDashboardAPIService(using AppContextProvider[AppConfig], AppContextProvider[SttpBackend[Future, Any]]) {

  private val logger = LoggerFactory.getLogger(classOf[ProofspaceDashboardAPIService])

  private val serviceInfoCache = new NetworkCache[ServiceInfo]()
  private val credDefsCache = new NetworkCache[IndyCredentialAndSchemaDTO]()

  import scala.concurrent.ExecutionContext.Implicits.global

  def getServiceInfo(serviceDid: String, network: String): Future[ServiceInfo] = async[Future] {
    serviceInfoCache.find(serviceDid, network) match
      case Some(serviceInfo) =>
        if (serviceInfo.expires.isAfter(OffsetDateTime.now())) then
          serviceInfo
        else
          val newServiceInfo = await(retrieveServiceInfo(serviceDid, network))
          serviceInfoCache.update(serviceDid, network, newServiceInfo)
          newServiceInfo
      case None =>
        val newServiceInfo = await(retrieveServiceInfo(serviceDid, network))
        serviceInfoCache.update(serviceDid, network, newServiceInfo)
        newServiceInfo
  }


  def clearServiceInfo(serviceDid: String, network:String): Future[Boolean] = async[Future] {
    serviceInfoCache.remove(serviceDid, network)
  }

  // this will be neede if we will issue credentials for voting.
  def issueCredentialSuccess(dto: WebhookCredentialIssuedDTO, network: String): Future[WebhookGenericReplyDTO] = async[Future] {
    val networkRecord =  AppContext[AppConfig].proofspace.networks.getOrElse(network, throw HttpException(StatusCode.InternalServerError, s"network ${network} is not configured"))
    val serviceInfo = await(getServiceInfo(dto.serviceDid, network))
    val serviceEndpointBase = serviceInfo.endpoint
    val serviceIssueCredentialUrl = s"${serviceEndpointBase}/webhook-accept/credentials-issued"

    val serviceIssueCredentialRequest = basicRequest.post(Uri.unsafeParse(serviceIssueCredentialUrl))
      .body(dto)
      .response(asJson[WebhookGenericReplyDTO])
      .sign(dto, serviceInfo, networkRecord)

    val sttpBackend = AppContext[SttpBackend[Future, Any]]
    val reply = await(serviceIssueCredentialRequest.send(sttpBackend))
    if (!reply.code.isSuccess) {
      logger.error(s"Can't call ${serviceIssueCredentialUrl}, code=${reply.code}, body=${reply.body}")
      logger.error(s"dto send: ${dto}")
      throw HttpException(StatusCode.ServiceUnavailable, s"Can't call ${serviceIssueCredentialUrl}, code=${reply.code}, body=${reply.body}")
    }
    reply.body match
      case Left(err) =>
        throw HttpException(StatusCode.ServiceUnavailable, "Can't call proofspace dashboard")
      case Right(dto) =>
        dto
  }


  def issueCredentialFailure(dto: WebhookCredentialIssueFailedDTO, network: String): Future[WebhookGenericReplyDTO] = async[Future] {
    val networkRecord = AppContext[AppConfig].proofspace.networks.getOrElse(network, throw HttpException(StatusCode.InternalServerError, s"network ${network} is not configured"))
    val serviceInfo = await(getServiceInfo(dto.serviceDid, network))
    val serviceEndpointBase = serviceInfo.endpoint
    val serviceIssueFailedUrl = s"${serviceEndpointBase}/webhook-accept/credentials-issuing-failed"
    val serviceIssueFailedRequest = basicRequest.post(Uri.unsafeParse(serviceIssueFailedUrl))
      .body(dto)
      .response(asJson[WebhookGenericReplyDTO])
      .sign(dto, serviceInfo, networkRecord)
    val sttpBackend = AppContext[SttpBackend[Future, Any]]
    val reply = await(serviceIssueFailedRequest.send(sttpBackend))
    if (!reply.code.isSuccess) {
      logger.error(s"Can't call ${serviceIssueFailedUrl}, code=${reply.code}")
      throw HttpException(StatusCode.ServiceUnavailable, s"Can't call ${serviceIssueFailedUrl}, code=${reply.code}, body=${reply.body}")
    }
    reply.body match
      case Left(err) =>
        throw HttpException(StatusCode.ServiceUnavailable, "Can't call proofspace dashboard")
      case Right(replyDto) =>
        replyDto
  }

  def getCredDef(credDefId: String, network: String): Future[IndyCredentialAndSchemaDTO] = async[Future] {
    credDefsCache.find(credDefId, network) match
      case Some(credDef) =>
        credDef
      case None =>
        val optCredDef = await(retrieveCredDef(credDefId, network))
        optCredDef match
          case Some(credDef) =>
            credDefsCache.update(credDefId, network, credDef)
            credDef
          case None =>
            throw HttpException(StatusCode.NotFound, s"credential definition ${credDefId} is not found")
  }

  protected def signRequest[U[_],T,R,B:JsonValueCodec](request: sttp.client3.RequestT[U,T,R], body:B, serviceInfo: ServiceInfo, networkRecord: ProofspaceNetworkConfig): sttp.client3.RequestT[U,T,R] = {
    networkRecord.requestSigning match
      case Some(dashboardKey) =>
        dashboardKey match
          case ProofspaceDashboardKey.SharedSecret(sharedSecret) =>
            request.auth.bearer(generateAuthJwtToken(serviceInfo, sharedSecret))
          case ProofspaceDashboardKey.RSAKey(key) =>
            val requestBytes = com.github.plokhotnyuk.jsoniter_scala.core.writeToArray(body)
            val md = MessageDigest.getInstance("SHA3-256")
            val digest = md.digest(requestBytes)
            val digestBase64 = Base64.getEncoder().encode(digest)
            request.header("X-Boody-Signature", new String(digestBase64))
      case None =>
        throw HttpException(StatusCode.InternalServerError, "request signing is not configured")
  }

  extension [U[_], T, R, B: JsonValueCodec](request: sttp.client3.RequestT[U,T,R])
    def sign(body:B, serviceInfo: ServiceInfo, networkRecord: ProofspaceNetworkConfig): sttp.client3.RequestT[U,T,R] = {
      signRequest(request, body, serviceInfo, networkRecord)
    }


  protected def retrieveServiceInfo(serviceDid: String, network: String): Future[ServiceInfo] = async[Future] {
    val networkConfig = AppContext[AppConfig].proofspace.networks.getOrElse(network,
      throw HttpException(StatusCode.InternalServerError, s"base admin url is not configurated for network ${network}")
    )
    val httpClientBackend =  AppContext[SttpBackend[Future, Any]]
    val baseZakaUrl = networkConfig.baseZakaUrl
    val serviceView = await(retrieveProofSpaceServiceViewForDid(serviceDid, baseZakaUrl))
    val publicKey = try {
      val pk = await(retrievePublicKeyForDid(serviceDid, serviceView.endpointsBase))
      Some(pk)
    } catch {
      case NonFatal(ex) =>
        logger.error(s"error during retrieving public key for ${serviceDid}, endpointsBase: ${serviceView.endpointsBase} network ${network}", ex)
        None
    }
    // TODO: expire in config.
    ServiceInfo(
      serviceDid,
      serviceView.endpointsBase,
      serviceView.name,
      serviceView.enabled,
      publicKey,
      OffsetDateTime.now(),
      OffsetDateTime.now().plusDays(1)
    )
  }

  protected def retrieveProofSpaceServiceViewForDid(serviceDid: String, baseZakaUrl: String): Future[ProofSpaceServiceViewDTO] = async[Future] {
    val serviceAdminInfoUrl = s"${baseZakaUrl}/partner-services/did/${serviceDid}"
    val serviceInfoRequest = basicRequest.get(Uri.unsafeParse(serviceAdminInfoUrl))
    val sttpBackend = AppContext[SttpBackend[Future, Any]]
    val reply = try
      await(serviceInfoRequest.send(sttpBackend))
    catch
      case NonFatal(e) =>
        logger.error(s"Can't call ${serviceAdminInfoUrl}", e)
        throw HttpException(StatusCode.ServiceUnavailable, s"Can't call ${serviceAdminInfoUrl}")
    if (!reply.code.isSuccess) {
      logger.error(s"Can't call ${serviceAdminInfoUrl}, code=${reply.code}")
      if (reply.code == StatusCode.NotFound) {
        throw HttpException(StatusCode.NotFound, s"dashboard for ${serviceDid} is not found, url=$serviceAdminInfoUrl")
      } else {
        throw HttpException(StatusCode.ServiceUnavailable, s"Can't call ${serviceAdminInfoUrl}, code=${reply.code}, body=${reply.body}")
      }
    }
    reply.body match
      case Left(err) =>
        throw HttpException(StatusCode.ServiceUnavailable, "Can't call ")
      case Right(body) =>
        val serviceView = readFromString[ProofSpaceServiceViewDTO](body)
        serviceView
  }

  protected def retrieveCredDef(credDefId: String, network: String): Future[Option[IndyCredentialAndSchemaDTO]] = async[Future] {
    val networkConfig = AppContext[AppConfig].proofspace.networks.getOrElse(network,
      throw HttpException(StatusCode.InternalServerError, s"base admin url is not configurated for network ${network}")
    )
    val httpClientBackend = AppContext[SttpBackend[Future, Any]]
    val baseZakaUrl = networkConfig.baseZakaUrl
    val credDefOwnerDid = credDefId.split(":").head

    val serviceInfo = await(getServiceInfo(credDefOwnerDid, network))
    val credentialInfoUrl = s"${serviceInfo.endpoint}/credential/id-extended/${credDefId}"
    val request = basicRequest.get(Uri.unsafeParse(credentialInfoUrl))
    val response = await(httpClientBackend.send(request))
    if (!response.code.isSuccess) {
      if (response.code == StatusCode.NotFound) {
        logger.debug(s"credential info not found for ${credDefId},  url=${credentialInfoUrl}")
        None
      } else {
        logger.error(s"unexpected response code ${response.code} from ${credentialInfoUrl}, body: ${response.body}")
        throw HttpException(StatusCode.ServiceUnavailable, s"failed to fetch credential info from ${credentialInfoUrl}")
      }
    } else {
      val credentialInfo = response.body match {
        case Left(error) =>
          logger.error(s"failed to fetch credential info from ${credentialInfoUrl}, error: ${error}")
          throw HttpException(StatusCode.ServiceUnavailable, s"failed to fetch credential info from ${credentialInfoUrl}")
        case Right(body) =>
          val dto: IndyCredentialAndSchemaDTO = readFromString[IndyCredentialAndSchemaDTO](body)
          dto
      }
      Some(credentialInfo)
    }

  }

  private def retrievePublicKeyForDid(serviceDid: String,
                                      containerServiceUrl: String): Future[RSAPublicKey] = async[Future] {
    val url = containerServiceUrl + "/public-info/public-key/default"
    val sttpBackend = AppContext[SttpBackend[Future, Any]]
    val reply = try
      logger.debug(s"retrievePublicKeyForDid: ${url}")
      await(basicRequest.get(Uri.unsafeParse(url)).send(sttpBackend))
    catch
      case NonFatal(e) =>
        logger.error(s"Can't call ${url}", e)
        throw HttpException(StatusCode.ServiceUnavailable, s"Can't call ${url}")
    if (!reply.code.isSuccess) then
      logger.error(s"Can't call ${url}, code=${reply.code}")
      throw HttpException(StatusCode.ServiceUnavailable, s"Can't call ${url}, code=${reply.code}, body=${reply.body}")
    reply.body match
      case Left(err) =>
        throw HttpException(StatusCode.ServiceUnavailable, s"Can't call ${url}: ${err}")
      case Right(body) =>
        val dto = try
          readFromString[ProofspaceContainerPublicKeyReplyDTO](body)
        catch
          case NonFatal(e) =>
            logger.error(s"Can't parse reply from ${url}", e)
            throw HttpException(StatusCode.ServiceUnavailable, s"Can't parse reply from ${url}")
        pemToRSAPublicKey(dto.publicKey, serviceDid);
  }

  private def pemToRSAPublicKey(pem: String, serviceDid: String): RSAPublicKey = {
    val retval = try
      val pemReader = new PemReader(new StringReader(pem));
      val pemObject = pemReader.readPemObject();
      val bytes = pemObject.getContent()
      val keySpec = new X509EncodedKeySpec(bytes)
      val keyFactory = KeyFactory.getInstance("RSA")
      keyFactory.generatePublic(keySpec)
    catch
      case NonFatal(e) =>
        logger.error(s"Can't convert pem to RSAPublicKey for service ${serviceDid}", e)
        throw HttpException(StatusCode.InternalServerError, s"Can't convert pem to RSAPublicKey for service ${serviceDid}")
    if (!retval.isInstanceOf[RSAPublicKey]) {
      throw HttpException(StatusCode.InternalServerError, s"Can't convert pem to RSAPublicKey for service ${serviceDid}, key is not RSAPublicKey but $retval")
    }
    retval.asInstanceOf[RSAPublicKey]
  }
  

  private def generateAuthJwtToken(info: ServiceInfo, sharedSecret: String): String = {
    val claim = JwtClaim(
      audience = Some(Set(info.proofspaceDid)),
      issuer = Some("proofspace-trustegistry"),
      expiration = Some(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 30),
    ).+("role", "issue-credential")
    Jwt.encode(claim, sharedSecret, JwtAlgorithm.HS256)
  }

}

