package proofspace.platform.dto


case class WebhookIntegrationOutputLogItemDTO(level: Int,
                                              message: String,
                                              additionalInfo: Map[String, String] = Map.empty)

case class WebhookIntegrationOutputDTO(integrationId: String,
                                       prevIntegrations: Seq[String],
                                       logData: Seq[WebhookIntegrationOutputLogItemDTO] = Seq.empty)

