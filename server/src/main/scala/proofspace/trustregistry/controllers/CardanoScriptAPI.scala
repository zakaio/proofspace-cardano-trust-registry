package proofspace.trustregistry.controllers

import com.bloxbean.cardano.client.address.AddressProvider
import com.bloxbean.cardano.client.plutus.spec.PlutusV3Script
import com.github.rssh.appcontext.{AppContext, AppContextProvider}
import org.slf4j.LoggerFactory
import proofspace.trustregistry.AppConfig
import proofspace.trustregistry.dto.*
import proofspace.trustregistry.gateways.cardano.contractTemplates.{CardanoContractsService, ScriptRepository}
import scalus.utils.Hex
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*
import sttp.tapir.server.*

import scala.concurrent.Future
import cps.*
import cps.monads.{*, given}
import proofspace.trustregistry.gateways.cardano.{BFCardanoOffchainAccess, BlockfrostSessions, CardanoHelper}
import proofspace.trustregistry.services.MongoDBService
import scalus.*

import scala.util.control.NonFatal


class CardanoScriptAPI(using AppContextProvider[ScriptRepository],
                         AppContextProvider[AppConfig],
                         AppContextProvider[CardanoContractsService]) extends BaseTapirController {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = LoggerFactory.getLogger(classOf[CardanoScriptAPI])

  lazy val endpoints: List[ServerEndpoint[Any,Future]] = List(
    uploadScript,
    generateTemplateScript,
    listTemplates
  )

  val uploadScript = endpoint.post
    .in("cardano" / "script")
    .securityIn(auth.bearer[Option[String]]())
    .securityIn(header[Option[String]]("X-Body-Signature"))
    .securityIn(query[Option[String]]("serviceDid"))
    .securityIn(query[Option[String]]("network"))
    .in(jsonBody[CardanoScriptUploadDTO])
    .out(jsonBody[CardanoScriptUploadResponseDTO])
    .errorOut(jsonBody[HttpExceptionDTO])
    .errorOut(statusCode)
    .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
    .description("Upload a Cardano script body")
    .serverSecurityLogic{
      case (optBearer, optSignature, optServiceDid, optNetwork) =>
        verifySignature(optBearer, optSignature, optServiceDid, optNetwork)
    }
    .serverLogic { case (serviceDid, network) => (dto) =>
      logger.info(s"Uploading script from dto: $dto")
      handleUploadScript(dto).map(Right(_)).recover{
        case ex:HttpException => Left(ex.toDTO)
      }
    }

  val generateTemplateScript = endpoint.post
    .in("cardano" / "script" / "from-template")
    .securityIn(auth.bearer[Option[String]]())
    .securityIn(header[Option[String]]("X-Body-Signature"))
    .securityIn(query[Option[String]]("serviceDid"))
    .securityIn(query[Option[String]]("network"))
    .in(jsonBody[CardanoContractGenerateDTO])
    .out(jsonBody[CardanoGenericContractDTO])
    .errorOut(jsonBody[HttpExceptionDTO])
    .errorOut(statusCode)
    .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
    .description("Generate addresses from a template")
    .serverSecurityLogic{
      case (optBearer, optSignature, optServiceDid, optNetwork) =>
        verifySignature(optBearer, optSignature, optServiceDid, optNetwork)
    }
    .serverLogic { case (serviceDid, network) => (dto) =>
      logger.info(s"Uploading script from dto: $dto")
      handleGenerateFromTemplate(dto, serviceDid, network).map(Right(_)).recover{
        case ex:HttpException => Left(ex.toDTO)
      }
    }

  val listTemplates = endpoint.get
    .in("cardano" / "script" / "templates")
    .securityIn(auth.bearer[Option[String]]())
    .securityIn(header[Option[String]]("X-Body-Signature"))
    .securityIn(query[Option[String]]("serviceDid"))
    .securityIn(query[Option[String]]("network"))
    .out(jsonBody[Seq[CardanoContractTemplateDTO]])
    .errorOut(jsonBody[HttpExceptionDTO])
    .errorOut(statusCode)
    .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
    .description("List available contract templates")
    .serverSecurityLogic{
      case (optBearer, optSignature, optServiceDid, optNetwork) =>
        verifySignature(optBearer, optSignature, optServiceDid, optNetwork)
     }
    .serverLogic{
      case (serviceDid, network) => _ =>
        logger.info("Listing available templates")
        val cardanoContractsService = AppContext[CardanoContractsService]
        val templates = cardanoContractsService.listTemplates()
        Future.successful(Right(templates))
    }

  def handleUploadScript(dto: CardanoScriptUploadDTO): Future[CardanoScriptUploadResponseDTO] = async[Future] {
      val byteArray = Hex.hexToBytes(dto.doubleCborHex)
      val plutusScript: PlutusV3Script =
        PlutusV3Script.builder()
          .`type`("PlutusScriptV3")
          .cborHex(dto.doubleCborHex)
          .build()
          .asInstanceOf[PlutusV3Script]
      val hash = Hex.bytesToHex(plutusScript.getScriptHash)
      val scriptRepository = AppContext[ScriptRepository]
      val u = await(scriptRepository.saveScript(hash, byteArray, dto.name))
      CardanoScriptUploadResponseDTO(hash)
  }

  def handleGenerateFromTemplate(dto: CardanoContractGenerateDTO, serviceDid: String, proofspaceNetwork: String): Future[CardanoGenericContractDTO] = async[Future] {
      val  cardanoContractsService = AppContext[CardanoContractsService]
      val contract = CardanoContractDTO
      val generatorFactory = cardanoContractsService.contractPlutusGenerator(dto.contract.templateName).getOrElse(
        throw HttpException(StatusCode.BadRequest, s"Template ${dto.contract.templateName} not found")
      )
      val scriptRepository = AppContext[ScriptRepository]
      val blockFrostSessions = AppContext[BlockfrostSessions]
      val bfBackend = blockFrostSessions.createBFService(serviceDid, dto.subnetwork, proofspaceNetwork)
      val cardanoOffchainAccess = BFCardanoOffchainAccess(bfBackend)
      val generator = generatorFactory(cardanoOffchainAccess)
      val targetAddressScript =
        try
          generator.generateTargetAddressScript(dto.contract.registryName, dto.contract.parameters)
        catch
          case NonFatal(ex) =>
            logger.warn(s"Failed to generate target address script: ${ex.getMessage}", ex)
            throw HttpException(StatusCode.BadRequest, s"Failed to generate target address script: ${ex.getMessage}")
      val targetCborHex = targetAddressScript.plutusV3.doubleCborHex
      val targetPlutusScript = PlutusV3Script.builder().cborHex(targetCborHex).build()
      val targetScriptHash = Hex.bytesToHex(targetPlutusScript.getScriptHash)
      saveIfNeeded(targetScriptHash, targetCborHex, s"target${dto.contract.registryName}").await
      val network = try
        CardanoHelper.asNetwork(dto.subnetwork)
      catch
        case NonFatal(ex) =>
          logger.warn(s"Failed to parse network: ${ex.getMessage}", ex)
          throw HttpException(StatusCode.BadRequest, s"Invalid cardano networkd ${dto.subnetwork}: ${ex.getMessage}")
      val targetAddress = AddressProvider.getEntAddress(targetPlutusScript, network)
      val targetSubmitPolicy: Option[String] = if (generator.hasApprovalProcess) then {
        val submitPolicy =
          try
            generator.generateSubmitMintingPolicy(dto.contract.registryName, dto.contract.parameters)
          catch
            case NonFatal(ex) =>
              logger.warn(s"Failed to generate submit minting policy: ${ex.getMessage}", ex)
              throw HttpException(StatusCode.BadRequest, s"Failed to generate submit minting policy: ${ex.getMessage}")
        val submitCborHex = submitPolicy.plutusV3.doubleCborHex
        val submitPlutusScript = PlutusV3Script.builder().cborHex(submitCborHex).build()
        val submitHash = Hex.bytesToHex(submitPlutusScript.getScriptHash)
        saveIfNeeded(submitHash, submitCborHex, s"submit${dto.contract.registryName}").await
        Some(submitHash)
      } else
        None
      val changeCost = generator.minChangeCost(dto.contract.parameters)
      val targetMintingPilict =
        try
          generator.generateTargetMintingPolicy(dto.contract.registryName, dto.contract.parameters)
        catch
          case NonFatal(ex) =>
            logger.warn(s"Failed to generate target minting policy: ${ex.getMessage}", ex)
            throw HttpException(StatusCode.BadRequest, s"Failed to generate target minting policy: ${ex.getMessage}")
      val targetMintingCborHex = targetMintingPilict.plutusV3.doubleCborHex
      val targetMintingPlutusScript = PlutusV3Script.builder().cborHex(targetMintingCborHex).build()
      val targetMintingHash = Hex.bytesToHex(targetMintingPlutusScript.getScriptHash)
      saveIfNeeded(targetMintingHash, targetMintingCborHex, s"minting${dto.contract.registryName}").await

      val votingTokens =
        try
          generator.votingTokens(dto.contract.parameters)
        catch
          case NonFatal(ex) =>
            logger.warn(s"Failed to generate voting tokens: ${ex.getMessage}", ex)
            throw HttpException(StatusCode.BadRequest, s"Failed to generate voting tokens: ${ex.getMessage}")

      CardanoGenericContractDTO(
        targetAddress = targetAddress.toBech32,
        changeSubmitCost = changeCost.toInt,
        targetMintingPolicy = targetMintingHash,
        submitMintingPolicy = targetSubmitPolicy,
        votingTokenPolicy = votingTokens.map(_.votingToken.toHex),
        votingTokenAsset = votingTokens.map(_.votingTokenAsset.toHex)
      )
  }

  private def saveIfNeeded(hash: String, cborHex: String, name: String): Future[Unit] = async[Future] {
    val scriptRepository = AppContext[ScriptRepository]
    val savedScript = scriptRepository.getScriptByHash(hash).await
    if (savedScript.isEmpty) then
      val byteArray = Hex.hexToBytes(cborHex)
      val u = scriptRepository.saveScript(hash, byteArray, Some(name))
  }


}
