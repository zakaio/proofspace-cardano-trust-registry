package proofspace.trustregistry.controllers

import scala.concurrent.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*
import com.github.rssh.appcontext.*
import org.slf4j.LoggerFactory
import proofspace.trustregistry.AppConfig
import proofspace.trustregistry.dto.*
import proofspace.trustregistry.gateways.TrustRegistryBackend
import proofspace.trustregistry.services.BlockchainAdapterService
import sttp.tapir.server.ServerEndpoint

class RegistryCrudAPI(using AppContextProvider[TrustRegistryBackend],
                            AppContextProvider[BlockchainAdapterService],
                            AppContextProvider[AppConfig]) extends BaseTapirController {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = LoggerFactory.getLogger(classOf[RegistryCrudAPI])


  lazy val endpoints: List[ServerEndpoint[Any,Future]] = List(
    listEndpoint,
    createEndpoint,
    removeEndpoint,
    submitChangeEndpoint,
    approveChangeEndpoint,
    rejectChangeEndpoint,
    queryEntriesEndpoint,
    checkDidEndpoint,
    networkChoiceEndpoing
  )


  val listEndpoint = endpoint.get.in("trust-registry")
    .securityIn(auth.bearer[Option[String]]())
    .securityIn(header[Option[String]]("X-Body-Signature"))
    .securityIn(query[Option[String]]("serviceDid"))
    .securityIn(query[Option[String]]("network"))
    .in(
      query[Option[String]]("registryId").description("registry-id").and(
           query[Option[String]]("name").description("Name of the registry")
        ).and(
           query[Option[Int]]("limit")
        ).and(
           query[Option[Int]]("offset")
        )
    ).mapIn((r,n,l,o) => TrustRegistryQueryDTO(r,n,l,o))(x => (x.registryId,x.name,x.limit,x.offset))
    .errorOut(jsonBody[HttpExceptionDTO])
    .errorOut(statusCode)
    .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
    .description("List trust registry entries")
    .out(jsonBody[TrustRegistriesDTO])
    .serverSecurityLogic { case (optBearer, optSignature, optServiceDid, optNetwork) =>
       verifySignature(optBearer, optSignature, optServiceDid, optNetwork)
    }.serverLogic { (serviceDid, proofspaceNetwork) => query =>
        handleListQuery(query, serviceDid, proofspaceNetwork)
    }

    val createEndpoint = endpoint.post.in("trust-registry")
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .securityIn(query[Option[String]]("serviceDid"))
      .securityIn(query[Option[String]]("network"))
      .in(jsonBody[CreateTrustRegistryDTO])
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Create a trust registry")
      .out(jsonBody[TrustRegistryDTO])
      .serverSecurityLogic { case (optBearer, optSignature, optServiceDid, optNetwork) =>
         verifySignature(optBearer, optSignature, optServiceDid, optNetwork)
      }.serverLogic { case (serviceDid, proofspaceNetwork) => create =>
          handleCreateRegistry(create.copy(proofspaceServiceDid = Some(serviceDid), proofspaceNetwork = Some(proofspaceNetwork)))
      }

    val removeEndpoint = endpoint.delete.in("trust-registry" / path[String]("registryId"))
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .securityIn(query[Option[String]]("serviceDid").description("Service DID"))
      .securityIn(query[Option[String]]("network").description("proofspace network"))
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Remove a trust registry")
      .out(jsonBody[Boolean])
      .serverSecurityLogic { case (optBearer, optSignature, optServiceDid, optNetwork) =>
         verifySignature(optBearer, optSignature, optServiceDid, optNetwork)
      }.serverLogic { (serviceDid, network) => registryId =>
          handleRemoveRegistry(registryId, serviceDid, network)
      }

    def submitChangeEndpoint = endpoint.post.in("trust-registry" / path[String]("registryId") / "change")
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .securityIn(query[Option[String]]("serviceDid").description("Service DID"))
      .securityIn(query[Option[String]]("network").description("proofspace network"))
      .in(jsonBody[TrustRegistryChangeDTO])
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Submit a change request to a trust registry")
      .out(jsonBody[TrustRegistryChangeDTO])
      .serverSecurityLogic { case (optBearer, optSignature, optServiceDid, optNetwork) =>
         verifySignature(optBearer, optSignature, optServiceDid, optNetwork)
      }.serverLogic { (serviceDid, proofspaceNetwork) => (registryId, change) =>
          handleSubmitChange(change, registryId, serviceDid, proofspaceNetwork)
      }

    def approveChangeEndpoint = endpoint.post.in("trust-registry" / path[String]("registryId") / "change" / path[String]("changeId") / "approve")
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .securityIn(query[Option[String]]("serviceDid").description("Service DID"))
      .securityIn(query[Option[String]]("network").description("proofspace network"))
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Approve a change request in a trust registry")
      .out(jsonBody[Boolean])
      .serverSecurityLogic { case (optBearer, optSignature, optServiceDid, optNetwork) =>
         verifySignature(optBearer, optSignature, optServiceDid, optNetwork)
      }.serverLogic { (serviceDid, proofspaceNetwork) => (registryId, changeId) =>
          handleApproveChange(registryId, changeId, serviceDid, proofspaceNetwork)
      }

    def rejectChangeEndpoint = endpoint.post.in("trust-registry" / path[String]("registryId") / "change" / path[String]("changeId") / "reject")
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .securityIn(query[Option[String]]("serviceDid").description("Service DID"))
      .securityIn(query[Option[String]]("network").description("proofspace network"))
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Reject a change request in a trust registry")
      .out(jsonBody[Boolean])
      .serverSecurityLogic { case (optBearer, optSignature, optServiceDid, optNetwork) =>
         verifySignature(optBearer, optSignature, optServiceDid, optNetwork)
      }.serverLogic { (serviceDid, proofspaceNetwork) => (registryId, changeId) =>
          handleRejectChange(registryId, changeId, serviceDid, proofspaceNetwork)
      }

    def queryEntriesEndpoint = endpoint.get
      .in("trust-registry" / path[String]("registryId") / "entries")
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .securityIn(query[Option[String]]("serviceDid").description("Service DID"))
      .securityIn(query[Option[String]]("network").description("proofspace network"))
      .in(
        query[Option[String]]("did").and(
             query[Option[Int]]("limit")
          ).and(
             query[Option[Int]]("offset")
          ).and(
            query[Option[String]]("orderBy")
          ).and(
            query[Option[String]]("orderByDirection")
        )
      ).mapIn(
        (registryId, did, limit, offset, orderBy, direction) =>
           TrustRegistryEntryQueryDTO(registryId,limit,offset, orderBy,direction, did))
        (x => (x.registryId, x.did, x.limit,x.offset, x.orderBy, x.orderByDirection))
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Query trust registry entries")
      .out(jsonBody[TrustRegistryDidEntriesDTO])
      .serverSecurityLogic { case (optBearer, optSignature, optServiceDid, optNetwork) =>
         verifySignature(optBearer, optSignature, optServiceDid, optNetwork)
      }.serverLogic { (serviceDid, network) => query =>
          handleQueryEntries(query, serviceDid, network)
      }

    val checkDidEndpoint = endpoint.get
      .in("trust-registry" / path[String]("registryId") / "did" / path[String]("did"))
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .securityIn(query[Option[String]]("serviceDid")).description("Service DID")
      .securityIn(query[Option[String]]("network")).description("proofspace network")
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Check if a DID is in a trust registry")
      .out(jsonBody[TrustRegistryDidEntryDTO])
      .serverSecurityLogic { case (optBearer, optSignature, optServiceDid, optNetwork) =>
         verifySignature(optBearer, optSignature, optServiceDid, optNetwork)
      }.serverLogic { (serviceDid, proofspaceNetwork) => (registryId, did) =>
          handleQueryDid(registryId, did)
      }

    val networkChoiceEndpoing = endpoint.get
      .in("network-choice")
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .securityIn(query[Option[String]]("serviceDid"))
      .securityIn(query[Option[String]]("network"))
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Get list of possible networks with subnetworks")
      .out(jsonBody[NetworkChoiceDTO])
      .serverSecurityLogic { case (optBearer, optSignature, optServiceDid, optNetwork) =>
         verifySignature(optBearer, optSignature, optServiceDid, optNetwork)
      }.serverLogic { case (serviceDid, network) => _ =>
          handleNetworkChoice()
      }

    def verifySignature(bearer: Option[String], signature: Option[String], optServiceDid: Option[String], optNetwork: Option[String]):
                              Future[Either[HttpExceptionDTO,(String,String)]] = {
      val proofspaceConfig = AppContext[AppConfig].proofspace
      val proofspaceNetwork = optNetwork.getOrElse(proofspaceConfig.defaultNetwork)
      val serviceDid = optServiceDid.getOrElse{
        proofspaceConfig.networks.get(proofspaceNetwork).flatMap(_.defaultServiceDid).getOrElse(
          throw HttpException(StatusCode.BadRequest, s"Service DID is not provided for network $proofspaceNetwork")
        )
      }
      Future.successful(Right[HttpExceptionDTO,(String,String)]((serviceDid, proofspaceNetwork)))
    }

    def handleListQuery(query: TrustRegistryQueryDTO, serviceDid: String, proofspaceNetwork: String): Future[Either[HttpExceptionDTO,TrustRegistriesDTO]] = {
      AppContext[TrustRegistryBackend].listRegistries(query, serviceDid, proofspaceNetwork).map(Right(_)).recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

    def handleCreateRegistry(create: CreateTrustRegistryDTO): Future[Either[HttpExceptionDTO,TrustRegistryDTO]] = {
      logger.debug(s"Creating registry ${create.name}")
      AppContext[TrustRegistryBackend].createRegistry(create).map(Right(_)).recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

    def handleRemoveRegistry(registryId: String, serviceDid: String, proofspaceNetwork: String): Future[Either[HttpExceptionDTO,Boolean]] = {
      logger.debug(s"Removing registry $registryId")
      AppContext[TrustRegistryBackend].removeRegistry(registryId, serviceDid, proofspaceNetwork).map(Right(_)).recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

    def handleSubmitChange(change: TrustRegistryChangeDTO, registryId: String, serviceDid: String, proofspaceNetwork: String): Future[Either[HttpExceptionDTO,TrustRegistryChangeDTO]] = {
      AppContext[TrustRegistryBackend].submitChange(change, serviceDid, proofspaceNetwork).map(Right(_)).recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

    def handleApproveChange(registryId: String, changeId: String, serviceDid: String, proofspaceNetwork: String): Future[Either[HttpExceptionDTO,Boolean]] = {
      AppContext[TrustRegistryBackend].approveChange(registryId, changeId, serviceDid, proofspaceNetwork).map(Right(_)).recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

    def handleRejectChange(registryId: String, changeId: String, serviceDid: String, proofspaceNetwork: String): Future[Either[HttpExceptionDTO,Boolean]] = {
      AppContext[TrustRegistryBackend].rejectChange(registryId, changeId, serviceDid, proofspaceNetwork).map(Right(_)).recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

    def handleQueryEntries(queryIn: TrustRegistryEntryQueryDTO, serviceDid: String, proofspaceNetwork: String): Future[Either[HttpExceptionDTO,TrustRegistryDidEntriesDTO]] = {
      val query = queryIn.copy(serviceDid = Some(serviceDid), proofspaceNetwork = Some(proofspaceNetwork))
      AppContext[TrustRegistryBackend].queryEntries(query).map(Right(_)).recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

    def handleQueryDid(registryId: String, did: String): Future[Either[HttpExceptionDTO,TrustRegistryDidEntryDTO]] = {
      AppContext[TrustRegistryBackend].queryDid(registryId, did).map {
        case Some(entry) => Right(entry)
        case None => Left(HttpExceptionDTO(StatusCode.NotFound.code, s"Entry with DID $did not found"))
      }.recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

    def handleNetworkChoice(): Future[Either[HttpExceptionDTO,NetworkChoiceDTO]] = {
      val networks = AppContext[BlockchainAdapterService].supportedNetworks
      Future.successful(Right(networks))
    }

  
}
