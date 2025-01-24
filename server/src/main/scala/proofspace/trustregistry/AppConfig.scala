package proofspace.trustregistry

case class CardanoConfig(
        blockfrost: Map[String, CardanoNetworkConfig]
                        )

case class CardanoNetworkConfig(
                          blockfrostUrl: String,
                          blockfrostProjectId: String,
                          blockfrostApiKey: String
                          )

case class AppConfig(
                    mongoUri: String,
                    mongoDbName: String,
                    cardanoConfig: CardanoConfig
                    )

