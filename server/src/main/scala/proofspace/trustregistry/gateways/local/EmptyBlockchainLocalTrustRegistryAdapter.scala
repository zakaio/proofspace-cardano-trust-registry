package proofspace.trustregistry.gateways.local

import proofspace.trustregistry.dto.CreateTrustRegistryDTO

import scala.concurrent.Future

class EmptyBlockchainLocalTrustRegistryAdapter extends BlockChainLocalTrustRegistryAdapter {

  override def createTrustRegistry(createTrustRegistryDTO: CreateTrustRegistryDTO): Future[String] = {
    val registryId = s"${createTrustRegistryDTO.network}:${createTrustRegistryDTO.name}"
    Future.successful(registryId)
  }
  
}
