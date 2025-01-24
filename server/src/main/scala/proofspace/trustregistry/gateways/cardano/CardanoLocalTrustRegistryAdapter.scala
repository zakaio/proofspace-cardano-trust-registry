package proofspace.trustregistry.gateways.cardano

import com.github.rssh.appcontext.AppContextProvider
import proofspace.trustregistry.{AppConfig, CardanoNetworkConfig}

import scala.concurrent.Future
import proofspace.trustregistry.dto.*
import proofspace.trustregistry.gateways.local.BlockChainLocalTrustRegistryAdapter

class CardanoLocalTrustRegistryAdapter(networkConfig: CardanoNetworkConfig) extends BlockChainLocalTrustRegistryAdapter {

  override def createTrustRegistry(createTrustRegistryDTO: CreateTrustRegistryDTO): Future[String] = {
     ???
  }

  override def createTrustRegistryChangeRequest(trustRegistryChangeDTO: TrustRegistryChangeDTO): Future[String] = {
      ???
  }

}
