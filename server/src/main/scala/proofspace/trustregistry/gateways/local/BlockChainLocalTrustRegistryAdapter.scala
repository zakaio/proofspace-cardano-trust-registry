package proofspace.trustregistry.gateways.local

import scala.concurrent.Future
import proofspace.trustregistry.dto.*

trait BlockChainAddressListener {
  def checkNewChange(change: TrustRegistryChangeDTO): Future[Boolean]
  def onAcceptedChange(changeId: String): Future[Boolean]
  def onRejectedChange(changeId: String): Future[Boolean]
}

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

  def monitorInputTransactions(addresses: List[String], listener: BlockChainAddressListener): Future[Unit]

}
