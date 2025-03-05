package proofspace.trustregistry

import metaconfig.*
import org.slf4j.LoggerFactory
import proofspace.platform.util.ProofspaceDashboardKey

case class CardanoConfig(
                          subnetworks: Map[String, CardanoNetworkConfig]
                        )

object CardanoConfig {

  lazy val default = CardanoConfig(
    subnetworks = Map(
      "mainnet" -> CardanoNetworkConfig(
        blockfrostUrl = "https://cardano-mainnet.blockfrost.io",
        blockfrostProjectId = "project_id",
        blockfrostApiKey = "api_key"
      ),
      "testnet" -> CardanoNetworkConfig(
        blockfrostUrl = "https://cardano-testnet.blockfrost.io",
        blockfrostProjectId = "project_id",
        blockfrostApiKey = "api_key"
      )
    )
  )

  implicit lazy val surface: generic.Surface[CardanoConfig] = generic.deriveSurface[CardanoConfig]
  implicit lazy val decoder: ConfDecoder[CardanoConfig] = generic.deriveDecoder[CardanoConfig](default)
  implicit lazy val encoder: ConfEncoder[CardanoConfig] = generic.deriveEncoder[CardanoConfig]

}

case class CardanoNetworkConfig(
                          blockfrostUrl: String,
                          blockfrostProjectId: String,
                          blockfrostApiKey: String
                          )

object CardanoNetworkConfig {

  lazy val default = CardanoNetworkConfig(
    blockfrostUrl = "https://cardano-mainnet.blockfrost.io",
    blockfrostProjectId = "project_id",
    blockfrostApiKey = "api_key"
  )

  implicit lazy val surface: generic.Surface[CardanoNetworkConfig] = generic.deriveSurface[CardanoNetworkConfig]
  implicit lazy val decoder: ConfDecoder[CardanoNetworkConfig] = generic.deriveDecoder[CardanoNetworkConfig](default)
  implicit lazy val encoder: ConfEncoder[CardanoNetworkConfig] = generic.deriveEncoder[CardanoNetworkConfig]

}

case class ProofspaceNetworkConfig(
                                  baseZakaUrl: String = "https://test.proofspace.id/zaka",
                                  requestSigning: Option[ProofspaceDashboardKey] = None,
                                  defaultServiceDid: Option[String] = None
                                  )
object ProofspaceNetworkConfig {
  lazy val default = ProofspaceNetworkConfig()
  implicit lazy val surface: generic.Surface[ProofspaceNetworkConfig] = generic.deriveSurface[ProofspaceNetworkConfig]
  implicit lazy val decoder: ConfDecoder[ProofspaceNetworkConfig] = generic.deriveDecoder[ProofspaceNetworkConfig](default)
  implicit lazy val encoder: ConfEncoder[ProofspaceNetworkConfig] = generic.deriveEncoder[ProofspaceNetworkConfig]
}

case class ProofspaceConfig(
                           defaultNetwork: String = "test",
                           networks: Map[String, ProofspaceNetworkConfig]
                           )

object ProofspaceConfig {
  lazy val default = ProofspaceConfig(
    defaultNetwork = "test",
    networks = Map(
      "test" -> ProofspaceNetworkConfig()
    )
  )
  implicit lazy val surface: generic.Surface[ProofspaceConfig] = generic.deriveSurface[ProofspaceConfig]
  implicit lazy val decoder: ConfDecoder[ProofspaceConfig] = generic.deriveDecoder[ProofspaceConfig](default)
  implicit lazy val encoder: ConfEncoder[ProofspaceConfig] = generic.deriveEncoder[ProofspaceConfig]
}

case class CardanoKeyConfig(
                           address: Option[String],
                           hash: Option[String],
                           seedPhrase: Option[String],
                           )

object CardanoKeyConfig {
  lazy val default = CardanoKeyConfig(
    address = Some("address"),
    hash = Some("hash"),
    seedPhrase = Some("seedPhrase")
  )
  implicit lazy val surface: generic.Surface[CardanoKeyConfig] = generic.deriveSurface[CardanoKeyConfig]
  implicit lazy val decoder: ConfDecoder[CardanoKeyConfig] = generic.deriveDecoder[CardanoKeyConfig](default)
  implicit lazy val encoder: ConfEncoder[CardanoKeyConfig] = generic.deriveEncoder[CardanoKeyConfig]
}

case class ExternalServiceConfig(
                                cardanoKeys: Map[String, CardanoKeyConfig] = Map.empty,
                                jwtSharedSecret: Option[String] = None
                                )

object ExternalServiceConfig {
  lazy val default = ExternalServiceConfig(
    cardanoKeys = Map(
      "mainnet" -> CardanoKeyConfig.default,
      "testnet" -> CardanoKeyConfig.default,
      "preprod" -> CardanoKeyConfig.default,
    )
  )
  implicit lazy val surface: generic.Surface[ExternalServiceConfig] = generic.deriveSurface[ExternalServiceConfig]
  implicit lazy val decoder: ConfDecoder[ExternalServiceConfig] = generic.deriveDecoder[ExternalServiceConfig](default)
  implicit lazy val encoder: ConfEncoder[ExternalServiceConfig] = generic.deriveEncoder[ExternalServiceConfig]
}

case class AppConfig(
                    mongoUri: String,
                    mongoDbName: String,
                    cardano: CardanoConfig,
                    host: String = "localhost",
                    port: Int = 4612,
                    encodingKeyFile: String = "enckeys.json",
                    proofspace: ProofspaceConfig = ProofspaceConfig.default,
                    externalServices: Map[String, ExternalServiceConfig] = Map.empty
                    )  {
  
  def retrieveProofspaceServiceDidAndNetwork(optServiceDid: Option[String], optProofspaceNetwork: Option[String]): (String, String) = {
    val network = optProofspaceNetwork.getOrElse(proofspace.defaultNetwork)
    val serviceDid = optServiceDid.getOrElse(
      proofspace.networks.getOrElse(network, 
          throw new IllegalStateException(s"network ${network} is not configured")
        ).defaultServiceDid.getOrElse(
          throw new IllegalStateException("serviceDid is not provided")
      )
    )
    (serviceDid, network)
  }
  
}

case class CmdLineConfig(
                          config: Option[String] = None,
                          configRequired: Boolean = false,
                          verbose: Boolean = false,
                        )

object CmdLineConfig {

  lazy val default = CmdLineConfig()
  implicit lazy val surface: generic.Surface[CmdLineConfig] = generic.deriveSurface[CmdLineConfig]
  implicit lazy val decoder: ConfDecoder[CmdLineConfig] = generic.deriveDecoder[CmdLineConfig](default)
  implicit lazy val encoder: ConfEncoder[CmdLineConfig] = generic.deriveEncoder[CmdLineConfig]

  def parse(args: Seq[String]): CmdLineConfig =
    Conf.parseCliArgs(args.toList).get.as[CmdLineConfig].get

}


object AppConfig {

  val logger = LoggerFactory.getLogger(classOf[AppConfig])

  lazy val default = AppConfig(
    mongoUri = "mongodb://localhost:27017",
    mongoDbName = "trustregistry",
    cardano = CardanoConfig.default
  )

  implicit lazy val surface: generic.Surface[AppConfig] = generic.deriveSurface[AppConfig]
  implicit lazy val decoder: ConfDecoder[AppConfig] = generic.deriveDecoder[AppConfig](default)
  implicit lazy val encoder: ConfEncoder[AppConfig] = generic.deriveEncoder[AppConfig]

  def read(cmdConfig: CmdLineConfig): AppConfig = {
    val fname = cmdConfig.config.getOrElse("./appConfig.json")
    readFile(new java.io.File(fname)) match
      case Some(config) =>
        logger.info(s"config file ${fname} read")
        config
      case None =>
        // trying to read .dot
        if (cmdConfig.configRequired) {
          throw new IllegalStateException(s"config file ${fname} not exists")
        }
        readFile(new java.io.File("./appConfig.json")) match
          case Some(config) => config
          case None =>
            default
  }


  def readFile(file: java.io.File): Option[AppConfig] = {
    if (!file.exists()) {
      logger.info(s"config file ${file} not exists")
      None
    } else {
      val input = Input.File(file)
      val retval = Hocon.fromInput(input).get.as[AppConfig].get
      Some(retval)
    }
  }

}

