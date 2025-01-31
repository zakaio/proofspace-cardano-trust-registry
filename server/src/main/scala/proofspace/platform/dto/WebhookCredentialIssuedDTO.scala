package proofspace.platform.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*


case class WebhookCredentialIssuedDTO(
                                       serviceDid: String,
                                       subscriberConnectDid: String,
                                       credentials: Seq[WebhookCredentialIssuedValuesDTO],
                                       issuedAt: Long,
                                       subscriberEventId: Option[String],
                                       actionTemplate: Option[String],
                                       actionParams: Option[Seq[NameValueDTO]],
                                       integrationOutput: WebhookIntegrationOutputDTO)

object WebhookCredentialIssuedDTO {
  implicit val jsonValueCodec: JsonValueCodec[WebhookCredentialIssuedDTO] = JsonCodecMaker.make
}

