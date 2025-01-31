package proofspace.platform.dto

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker


case class WebhookCredentialIssueFailedDTO(
                                            serviceDid: String,
                                            subscriberConnectDid: String,
                                            errorMessage: String,
                                            integrationOutput: WebhookIntegrationOutputDTO,
                                            credentialsIds: Seq[String],
                                            subscriberEventId: Option[String],
                                            actionTemplate: Option[String] = None)

object WebhookCredentialIssueFailedDTO {

  implicit val codec: JsonValueCodec[WebhookCredentialIssueFailedDTO] = JsonCodecMaker.make

}

