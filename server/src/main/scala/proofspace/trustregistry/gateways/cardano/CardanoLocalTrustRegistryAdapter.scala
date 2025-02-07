package proofspace.trustregistry.gateways.cardano

import com.bloxbean.cardano.client.plutus.spec.PlutusData
import com.github.rssh.appcontext.{AppContext, AppContextProvider}
import proofspace.trustregistry.{AppConfig, CardanoNetworkConfig}

import scala.concurrent.Future
import proofspace.trustregistry.dto.*
import proofspace.trustregistry.gateways.local.{BlockChainAddressListener, BlockChainLocalTrustRegistryAdapter}
import com.bloxbean.cardano.client.transaction.spec.{Transaction}
import com.bloxbean.cardano.client.plutus.spec.PlutusData

class CardanoLocalTrustRegistryAdapter(cardanoSubnetwork: String, cardanoNetworkConfig: CardanoNetworkConfig)(
  using AppContextProvider[AppConfig]) extends BlockChainLocalTrustRegistryAdapter {

  override def createTrustRegistry(createTrustRegistryDTO: CreateTrustRegistryDTO, serviceDid: String, proofspaceNetwork: String): Future[String] = {

    val transactionService = AppContext[BlockfrostSessions].createTransactionService(serviceDid, cardanoSubnetwork, proofspaceNetwork)
    val senderAddress = retrieveSenderAddress(serviceDid, cardanoSubnetwork, proofspaceNetwork)
    val receiverAddress = createTrustRegistryDTO.createTargetAddress.getOrElse(throw IllegalArgumentException("Target address for cardano is required"))
    val amountToSend = createTrustRegistryDTO.createSubmitCost.getOrElse(throw IllegalArgumentException("Submit cost for cardano is required"));

    //val datum = PlutusData.deserialize(createTrustRegistryDTO)
     ???
  }

  override def createTrustRegistryChangeRequest(trustRegistryChangeDTO: TrustRegistryChangeDTO, serviceDid: String, proofpsaceNetwork: String): Future[String] = {
      ???
  }

  def monitorInputTransactions(addresses: List[String], listener: BlockChainAddressListener, serviceDid: String, proofspaceNetwork: String): Future[Unit] = {
    ???
  }

  private def retrieveSenderAddress(serviceDid: String, cardanoSubnetwork: String, proofspaceNetwork: String): String = {
    val appConfig = AppContext[AppConfig]
    appConfig.externalServices.get(serviceDid) match
      case Some(serviceConfig) =>
        serviceConfig.cardanoKeys.getOrElse(cardanoSubnetwork,
          throw IllegalArgumentException(s"Cardano subnetwork $cardanoSubnetwork not found in config")
        ).address.getOrElse(
          throw IllegalArgumentException(s"Cardano address for subnetwork $cardanoSubnetwork not found in config")
        )
      case None =>
        //TODO: retrieve from proofspace config.
        throw IllegalArgumentException(s"Service $serviceDid not found in config, proofspace search is not implemented yet")
  }

}
