package proofspace.trustregistry.gateways.local

import proofspace.trustregistry.dto.*

import scala.concurrent.Future

class EmptyBlockchainLocalTrustRegistryAdapter extends BlockChainLocalTrustRegistryAdapter {

  override def createTrustRegistry(createTrustRegistryDTO: CreateTrustRegistryDTO): Future[String] = {
    val registryId = s"${createTrustRegistryDTO.network}:${createTrustRegistryDTO.name}"
    Future.successful(registryId)
  }

  override def createTrustRegistryChangeRequest(trustRegistryChangeDTO: TrustRegistryChangeDTO): Future[String] = {
    Future.successful(trustRegistryChangeDTO.changeId.getOrElse(java.util.UUID.randomUUID().toString))
  }
  
}
