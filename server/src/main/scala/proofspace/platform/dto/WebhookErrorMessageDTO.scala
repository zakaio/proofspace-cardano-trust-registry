package proofspace.platform.dto

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker


case class WebhookErrorMessageDTO(
                                   val message: String,
                                   val details: Option[String] = None
                                 )

object WebhookErrorMessageDTO {

  implicit val codec: JsonValueCodec[WebhookErrorMessageDTO] = JsonCodecMaker.make

}

