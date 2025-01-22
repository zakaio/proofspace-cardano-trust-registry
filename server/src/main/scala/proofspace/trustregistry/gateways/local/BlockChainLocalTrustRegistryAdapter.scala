package proofspace.trustregistry.gateways.local

import scala.concurrent.Future
import proofspace.trustregistry.dto.*

/**
 * Local adapter, which allow relfect local changes in blockchain
 * or build local versio ftrust-regostry from blockchain
 */
trait BlockChainLocalTrustRegistryAdapter  {

  /**
   * Allocate trust registry id.
   * @param createTrustRegistryDTO
   * @return
   */
  def createTrustRegistry(createTrustRegistryDTO: CreateTrustRegistryDTO): Future[String]

  def createTrustRegistryChangeRequest(trustRegistryChangeDTO: TrustRegistryChangeDTO): Future[String]


}
