package proofspace.trustregistry.controllers

import com.github.plokhotnyuk.jsoniter_scala.core.*
import org.slf4j.LoggerFactory
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*


class BaseTapirController {

  implicit val schemaForStatusCode: Schema[StatusCode] = Schema.schemaForInt.map[StatusCode](code => Some(StatusCode.unsafeApply(code)))(_.code)

  given JsonValueCodec[Boolean] = new JsonValueCodec[Boolean] {
    override def decodeValue(in: JsonReader, default: Boolean): Boolean = in.readBoolean()
    override def encodeValue(x: Boolean, out: JsonWriter): Unit = out.writeVal(x)
    override def nullValue: Boolean = false
  }

}
