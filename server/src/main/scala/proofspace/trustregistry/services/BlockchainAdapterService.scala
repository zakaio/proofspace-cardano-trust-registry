package proofspace.trustregistry.services

import com.github.rssh.appcontext.*
import proofspace.trustregistry.AppConfig
import proofspace.trustregistry.gateways.cardano.CardanoLocalTrustRegistryAdapter
import proofspace.trustregistry.gateways.local.{BlockChainLocalTrustRegistryAdapter, EmptyBlockchainLocalTrustRegistryAdapter}

class BlockchainAdapterService(using AppContextProvider[AppConfig]) {

  def createBlockchainAdapter(network: String, subnetwork:Option[String]): BlockChainLocalTrustRegistryAdapter = {
    network match
      case "local" => new EmptyBlockchainLocalTrustRegistryAdapter()
      case "cardano" =>
        val cardanoNetwork = subnetwork.getOrElse("mainnet")
        AppContext[AppConfig].cardanoConfig.subnetworks.get(cardanoNetwork) match
          case Some(networkConfig) => new CardanoLocalTrustRegistryAdapter(networkConfig)
          case None => throw new IllegalArgumentException(s"Cardano network $cardanoNetwork not found")
  }

}

object BlockchainAdapterService {

  given (using AppContextProviders[(AppConfig, AppContext.Cache)]): AppContextProvider[BlockchainAdapterService] =
    new AppContextProvider[BlockchainAdapterService] {
      override def get: BlockchainAdapterService =
        AppContext[AppContext.Cache].getOrCreate(new BlockchainAdapterService)
    }


}