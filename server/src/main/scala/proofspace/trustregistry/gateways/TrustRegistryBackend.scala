package proofspace.trustregistry.gateways

import scala.concurrent.Future


import proofspace.trustregistry.dto.*


trait TrustRegistryBackend {
  
  def name: String
  
  def createRegistry(create: CreateTrustRegistryDTO): Future[TrustRegistryDTO]
  
  def submitChange(change: TrustRegistryChangeDTO): Future[Unit]
  
  def queryEntries(query: TrustRegistryEntryQueryDTO): Future[TrustRegistryEntriesDTO]
  
  def queryDid(registryId: String, did: String): Future[Option[TrustRegistryEntryDTO]]
  
}
