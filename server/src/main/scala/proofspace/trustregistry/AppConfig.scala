package proofspace.trustregistry

import metaconfig.*
import org.slf4j.LoggerFactory

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

case class AppConfig(
                    mongoUri: String,
                    mongoDbName: String,
                    cardano: CardanoConfig,
                    host: String = "localhost",
                    port: Int = 4612,
                    proofspaceApi: String = "https://test.proofspace.id/zaka"
                    )

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

