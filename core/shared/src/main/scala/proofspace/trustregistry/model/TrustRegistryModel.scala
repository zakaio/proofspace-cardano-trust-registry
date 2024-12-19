package proofspace.trustregistry.model

import scalus.builtin.*
import scalus.ledger.api.v3.PosixTime
import scalus.ledger.api.v3.TxId

/**
 * Here is a representation of trust registry, which exists in the offline
 * world.  (and normally is extracted from the blockchain ledger)
 */
trait TrustRegistrySnapshot {

  /**
   * Id of trust-regostry: this is id of the transaction, which created trust registry.
   * @return
   */
  def id: TxId

  /**
   * Id of the last modification of the trust registry.
   * @return
   */
  def lastTouch: PosixTime;

  /**
   * Id of the last transaction, which modified the trust registry.
   */
  def lastTxId: TxId

  /**
   * name of the trust registry
   */
  def name: String

  /**
   * Check, if the given DID is in the trust registry.
   */
  def check(did: String): Boolean

  /**
   * Apply change to the trust registry and return new snapshot.
   */
  def applyChange(change: TrustRegistryChange): TrustRegistrySnapshot

  /**
   * retrieve current maintance model of the trust registry.
   */
  def maintanceModel: TrustRegistryMaintanceModel
  
}


/**
 * Change describe by the set of the trust registry operation.
 * Mapping from transaction to changed is defined by maintance model.
 */
case class TrustRegistryChange(id: TxId, registryId: TxId, operations: List[TrustRegistryOperation], time: PosixTime)


enum TrustRegistryOperation {

  case AddDid(did: ByteString)
  case RemoveDid(did: ByteString)
  case ChangeMaintanceModel(newMaintanceModel: TrustRegistryMaintanceModel)
  
  
}



