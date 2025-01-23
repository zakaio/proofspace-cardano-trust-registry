package proofspace.trustregistry.gateways

import scala.concurrent.Future


import proofspace.trustregistry.dto.*


trait TrustRegistryBackend {
  
  def name: String
  
  def createRegistry(create: CreateTrustRegistryDTO): Future[TrustRegistryDTO]
  
  def submitChange(change: TrustRegistryChangeDTO): Future[TrustRegistryChangeDTO]
  
  def rejectChange(registryId: String, changeId: String): Future[Boolean]
  
  def approveChange(registryId: String, changeId: String): Future[Boolean]
  
  def queryEntries(query: TrustRegistryEntryQueryDTO): Future[TrustRegistryDidEntriesDTO]
  
  def queryDid(registryId: String, did: String): Future[Option[TrustRegistryDidEntryDTO]]
  
}
