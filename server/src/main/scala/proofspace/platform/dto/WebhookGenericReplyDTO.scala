package proofspace.platform.dto

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

case class WebhookGenericReplyDTO(
                                   protocolVersion: Int = 1,
                                   ok: Boolean,
                                   error: Option[WebhookErrorMessageDTO] = None)

object WebhookGenericReplyDTO {

  given JsonValueCodec[WebhookGenericReplyDTO] = JsonCodecMaker.make

}

