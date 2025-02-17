package proofspace.trustregistry.gateways.cardano

import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService
import com.bloxbean.cardano.client.backend.api.TransactionService
import com.github.rssh.appcontext.{AppContext, AppContextProvider}
import proofspace.trustregistry.AppConfig


class BlockfrostSessions(using AppContextProvider[AppConfig]) {

  
  def createBFService(serviceDid: String, cardanoSubnetwork: String, proofspaceNetwork: String): BFBackendService = {
    val (baseUrl, apiKey) = AppContext[AppConfig].cardano.subnetworks.get(cardanoSubnetwork) match
      case Some(subnetworkConfig) =>
        (subnetworkConfig.blockfrostUrl, subnetworkConfig.blockfrostApiKey)
      case None => throw IllegalArgumentException(s"Cardano subnetwork $cardanoSubnetwork not found in config")
    new BFBackendService(baseUrl, apiKey)
  }
  
  def createTransactionService(serviceDid: String, cardanoSubnetwork: String, proofspaceNetwork: String): TransactionService = {
     val (baseUrl, apiKey) = AppContext[AppConfig].cardano.subnetworks.get(cardanoSubnetwork) match
       case Some(subnetworkConfig) =>
         (subnetworkConfig.blockfrostUrl, subnetworkConfig.blockfrostApiKey)
       case None => throw IllegalArgumentException(s"Cardano subnetwork $cardanoSubnetwork not found in config")
     val backendService = new BFBackendService(baseUrl, apiKey)
     backendService.getTransactionService
  }


  /*
  private def retrieveCardanoKey(serviceDid: String, cardanoSubnetwork: String, proofspaceNetwork: String): String = {
    // at first, see in local config file.
    val appConfig = AppContext[AppConfig]
    appConfig.externalServices.get(serviceDid) match
      case Some(serviceConfig) =>
        serviceConfig.cardanoKeys.get(cardanoSubnetwork) match
          case Some(key) => key
  }*/

}

object BlockfrostSessions {
  
  given (using AppContextProvider[AppConfig]): AppContextProvider[BlockfrostSessions] = new AppContextProvider[BlockfrostSessions] {
    def get: BlockfrostSessions = new BlockfrostSessions
  }
  
}
