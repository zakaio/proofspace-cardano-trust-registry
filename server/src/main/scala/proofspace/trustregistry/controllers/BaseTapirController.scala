package proofspace.trustregistry.controllers

import scala.concurrent.Future
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import com.github.rssh.appcontext.{AppContext, AppContextProvider}
import org.slf4j.LoggerFactory
import proofspace.trustregistry.AppConfig
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*


class BaseTapirController(using AppContextProvider[AppConfig]) {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val schemaForStatusCode: Schema[StatusCode] = Schema.schemaForInt.map[StatusCode](code => Some(StatusCode.unsafeApply(code)))(_.code)

  given JsonValueCodec[Boolean] = new JsonValueCodec[Boolean] {
    override def decodeValue(in: JsonReader, default: Boolean): Boolean = in.readBoolean()
    override def encodeValue(x: Boolean, out: JsonWriter): Unit = out.writeVal(x)
    override def nullValue: Boolean = false
  }

  given seqValueCodec[T](using JsonValueCodec[T]):JsonValueCodec[Seq[T]] = JsonCodecMaker.make


  protected def verifySignature(bearer: Option[String], signature: Option[String], optServiceDid: Option[String], optNetwork: Option[String]):
    Future[Either[HttpExceptionDTO, (String, String)]] = {
      val proofspaceConfig = AppContext[AppConfig].proofspace
      val proofspaceNetwork = optNetwork.getOrElse(proofspaceConfig.defaultNetwork)
      val serviceDid = optServiceDid.getOrElse {
        proofspaceConfig.networks.get(proofspaceNetwork).flatMap(_.defaultServiceDid).getOrElse(
          throw HttpException(StatusCode.BadRequest, s"Service DID is not provided for network $proofspaceNetwork")
        )
      }
      Future.successful(Right[HttpExceptionDTO, (String, String)]((serviceDid, proofspaceNetwork)))
  }


}
