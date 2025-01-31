package proofspace.platform.dto

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.tapir.*
import sttp.tapir.generic.auto.*


case class WebhookCredentialIssuedValuesDTO(credentialId: String,
                                            credValId: Option[String],
                                            fields: Seq[NameValueDTO],
                                            utcIssuedAt: Option[Long],
                                            //revoked: Boolean,
                                            //utcRevokedAt: Option[Long],
                                            //extraPublishInfo: Option[Any]
                                           )

object WebhookCredentialIssuedValuesDTO {
  implicit val codec: JsonValueCodec[WebhookCredentialIssuedValuesDTO] = JsonCodecMaker.make[WebhookCredentialIssuedValuesDTO]
  implicit lazy val schema: Schema[WebhookCredentialIssuedValuesDTO] = Schema.derived
}

