package proofspace.trustregistry.gateways.local

import scala.concurrent.Future

import proofspace.trustregistry.dto.*
import proofspace.trustregistry.gateways.TrustRegistryBackend

/**
 * Local trust registry backend without blockchain backing.
 * All changes to the trust registry are applied immediatly without voting.
 */
class MongoDBTrustRegistryBackend extends TrustRegistryBackend {

  override def name: String = "MongoDBTrustRegistryBackend"

  override def createRegistry(create: CreateTrustRegistryDTO): Future[TrustRegistryDTO] = ???

  override def submitChange(change: TrustRegistryChangeDTO): Future[Unit] = ???

  override def queryEntries(query: TrustRegistryEntryQueryDTO): Future[TrustRegistryEntriesDTO] = ???

  override def queryDid(registryId: String, did: String): Future[Option[TrustRegistryEntryDTO]] = ???

}
