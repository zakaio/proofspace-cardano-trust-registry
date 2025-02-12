package proofspace.trustregistry.services

import com.github.rssh.appcontext.*
import org.slf4j.LoggerFactory
import proofspace.trustregistry.AppConfig
import proofspace.trustregistry.dto.{NetworkChoiceDTO, NetworkChoiceItemDTO}
import proofspace.trustregistry.gateways.cardano.CardanoLocalTrustRegistryAdapter
import proofspace.trustregistry.gateways.local.{BlockChainLocalTrustRegistryAdapter, EmptyBlockchainLocalTrustRegistryAdapter}

class BlockchainAdapterService(using AppContextProvider[AppConfig]) {

  val logger = LoggerFactory.getLogger(classOf[BlockchainAdapterService])

  def supportedNetworks: NetworkChoiceDTO = {
    val appConfig = AppContext[AppConfig]
    NetworkChoiceDTO(Seq(
       NetworkChoiceItemDTO("local", Seq()),
       NetworkChoiceItemDTO("cardano", appConfig.cardano.subnetworks.keys.toSeq)
    ))
  }
  
  def createBlockchainAdapter(network: String, subnetwork:Option[String]): BlockChainLocalTrustRegistryAdapter = {
    network match
      case "local" =>
        new EmptyBlockchainLocalTrustRegistryAdapter()
      case "cardano" =>
        val cardanoNetwork = subnetwork.getOrElse("mainnet")
        AppContext[AppConfig].cardano.subnetworks.get(cardanoNetwork) match
          case Some(networkConfig) => new CardanoLocalTrustRegistryAdapter(cardanoNetwork, networkConfig)
          case None =>
            logger.warn(s"Cardano network ${cardanoNetwork} not found")
            throw new IllegalArgumentException(s"Cardano network $cardanoNetwork not found")
  }

}

object BlockchainAdapterService {

  given (using AppContextProviders[(AppConfig, AppContext.Cache)]): AppContextProvider[BlockchainAdapterService] =
    new AppContextProvider[BlockchainAdapterService] {
      override def get: BlockchainAdapterService =
        AppContext[AppContext.Cache].getOrCreate(new BlockchainAdapterService)
    }


}