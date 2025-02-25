package proofspace.trustregistry.controllers

import com.bloxbean.cardano.client.plutus.spec.PlutusV3Script
import com.github.rssh.appcontext.{AppContext, AppContextProvider}
import org.slf4j.LoggerFactory
import proofspace.trustregistry.AppConfig
import proofspace.trustregistry.dto.{CardanoScriptUploadDTO, CardanoScriptUploadResponseDTO}
import proofspace.trustregistry.gateways.cardano.contractTemplates.ScriptRepository
import scalus.utils.Hex
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*
import sttp.tapir.server.*

import scala.concurrent.Future
import cps.*
import cps.monads.{*, given}



class CardanoScriptAPI(using AppContextProvider[ScriptRepository],
                         AppContextProvider[AppConfig]) extends BaseTapirController {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = LoggerFactory.getLogger(classOf[CardanoScriptAPI])

  lazy val endpoints: List[ServerEndpoint[Any,Future]] = List(
    uploadScript
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

}
