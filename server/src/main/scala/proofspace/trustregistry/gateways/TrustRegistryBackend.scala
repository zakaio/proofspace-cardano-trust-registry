package proofspace.trustregistry.gateways

import scala.concurrent.Future


import proofspace.trustregistry.dto.*


trait TrustRegistryBackend {
  
  def name: String
  
  def createRegistry(create: CreateTrustRegistryDTO): Future[TrustRegistryDTO]
  
  def submitChange(change: TrustRegistryChangeDTO): Future[TrustRegistryChangeDTO]
  
  def rejectChange(changeId: String): Future[Unit]
  
  def approveChange(changeId: String): Future[Unit]
  
  def queryEntries(query: TrustRegistryEntryQueryDTO): Future[TrustRegistryDidEntriesDTO]
  
  def queryDid(registryId: String, did: String): Future[Option[TrustRegistryDidEntryDTO]]
  
}
