package proofspace.trustregistry.gateways.local

import proofspace.trustregistry.dto.*

import scala.concurrent.Future

class EmptyBlockchainLocalTrustRegistryAdapter extends BlockChainLocalTrustRegistryAdapter {

  override def createTrustRegistry(createTrustRegistryDTO: CreateTrustRegistryDTO, serviceDid: String, proofspaceNetwork:String): Future[String] = {
    val registryId = s"${createTrustRegistryDTO.network}:${createTrustRegistryDTO.name}"
    Future.successful(registryId)
  }

  override def createTrustRegistryChangeRequest(trustRegistryChangeDTO: TrustRegistryChangeDTO, serviceDid: String, proofspaceNetwork: String): Future[String] = {
    Future.successful(trustRegistryChangeDTO.changeId.getOrElse(java.util.UUID.randomUUID().toString))
  }

  def monitorInputTransactions(addresses: List[String], listener: BlockChainAddressListener, serviceDid: String, proofspaceNetwork: String): Future[Unit] = {
    Future.successful(())
  }
  
  
}
