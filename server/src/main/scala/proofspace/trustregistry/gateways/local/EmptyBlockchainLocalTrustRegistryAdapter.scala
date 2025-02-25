package proofspace.trustregistry.gateways.local

import proofspace.trustregistry.dto.*

import scala.concurrent.Future

class EmptyBlockchainLocalTrustRegistryAdapter extends BlockChainLocalTrustRegistryAdapter {

  override def createTrustRegistry(createTrustRegistryDTO: CreateTrustRegistryDTO, serviceDid: String, proofspaceNetwork:String): Future[BlockChainLocalTrustRegistryAdapter.CreateResult] = {
    val registryId = s"${createTrustRegistryDTO.network}:${createTrustRegistryDTO.name}"
    val retval = BlockChainLocalTrustRegistryAdapter.CreateResult(registryId, registryId)
    Future.successful(retval)
  }

  override def createTrustRegistryChangeRequest(trustRegistryChangeDTO: TrustRegistryChangeDTO, serviceDid: String, proofspaceNetwork: String): Future[String] = {
    Future.successful(trustRegistryChangeDTO.changeId.getOrElse(java.util.UUID.randomUUID().toString))
  }

  def monitorInputTransactions(addresses: List[String], listener: BlockChainAddressListener, serviceDid: String, proofspaceNetwork: String): Future[Unit] = {
    Future.successful(())
  }
  
  
}
