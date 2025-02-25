package proofspace.trustregistry.gateways.cardano

import com.bloxbean.cardano.client.plutus.spec.PlutusData
import com.github.rssh.appcontext.{AppContext, AppContextProvider}
import proofspace.trustregistry.{AppConfig, CardanoKeyConfig, CardanoNetworkConfig}

import scala.concurrent.Future
import proofspace.trustregistry.dto.*
import proofspace.trustregistry.gateways.local.{BlockChainAddressListener, BlockChainLocalTrustRegistryAdapter}
import com.bloxbean.cardano.client.transaction.spec.Transaction
import com.bloxbean.cardano.client.plutus.spec.PlutusData
import proofspace.trustregistry.gateways.cardano.contractTemplates.*
import proofspace.trustregistry.offchain.ContractGenerator
import proofspace.trustregistry.services.MongoDBService
import scalus.bloxbean.Interop

class CardanoLocalTrustRegistryAdapter(cardanoSubnetwork: String, cardanoNetworkConfig: CardanoNetworkConfig)(
  using AppContextProvider[AppConfig],
        AppContextProvider[BlockfrostSessions],
        AppContextProvider[MongoDBService]
) extends BlockChainLocalTrustRegistryAdapter {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def createTrustRegistry(createTrustRegistryDTO: CreateTrustRegistryDTO, serviceDid: String, proofspaceNetwork: String): Future[BlockChainLocalTrustRegistryAdapter.CreateResult] = {
    val cardanoSubnetwork = createTrustRegistryDTO.subnetwork.getOrElse(
      throw IllegalArgumentException("Cardano subnetwork is required")
    )
    val senderAddress = retrieveSenderAddress(serviceDid, cardanoSubnetwork)
    val cardanoKeys = retrieveCardanoKeys(serviceDid, cardanoSubnetwork)
    val cardanoContractParam = createTrustRegistryDTO.cardano.getOrElse(throw IllegalArgumentException("Cardano contract is required"))
    val bfService = AppContext[BlockfrostSessions].createBFService(serviceDid, cardanoSubnetwork, proofspaceNetwork)
    val transactionBuilder = cardanoContractParam match
      case CardanoCreateParams.Generic(contract) =>
        new GenericContractTransactionBuilder(contract, bfService, senderAddress, cardanoKeys, AppContext[ScriptRepository])
      case CardanoCreateParams.Template(contract) =>
        val cardanoContractsServide = AppContext[CardanoContractsService]
        cardanoContractsServide.getTransactionBuilder(contract, bfService, senderAddress, cardanoKeys).getOrElse(
          throw IllegalArgumentException(s"Template ${contract.templateName} not found")
        )
    val fTx = transactionBuilder.buildCreateTransaction(createTrustRegistryDTO.name, cardanoSubnetwork).map(_.toString)
    val fTargetAddress = transactionBuilder.buildTargetAddress(createTrustRegistryDTO.name, cardanoSubnetwork)
    for(tx <- fTx; targetAddress <- fTargetAddress) yield BlockChainLocalTrustRegistryAdapter.CreateResult(tx, targetAddress)
  }

  override def createTrustRegistryChangeRequest(trustRegistryChangeDTO: TrustRegistryChangeDTO, serviceDid: String, proofpsaceNetwork: String): Future[String] = {
      ???
  }

  def monitorInputTransactions(addresses: List[String], listener: BlockChainAddressListener, serviceDid: String, proofspaceNetwork: String): Future[Unit] = {
    ???
  }

  private def retrieveSenderAddress(serviceDid: String, cardanoSubnetwork: String): String = {
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

  private def retrieveCardanoKeys(serviceDid: String, cardanoSubnetwork: String): Map[String, CardanoKeyConfig] = {
    val appConfig = AppContext[AppConfig]
    appConfig.externalServices.get(serviceDid) match
      case Some(serviceConfig) =>
        serviceConfig.cardanoKeys
      case None =>
        throw IllegalArgumentException(s"Service $serviceDid not found in config, proofspace search is not implemented yet")
  }

}
