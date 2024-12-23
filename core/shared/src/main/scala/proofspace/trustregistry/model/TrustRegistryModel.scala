package proofspace.trustregistry.model

import scalus.builtin.*
import scalus.builtin.Data.{FromData, ToData}
import scalus.builtin.ToDataInstances.given
import scalus.builtin.FromDataInstances.given
import scalus.ledger.api.v3.{*, given}
import scalus.ledger.api.v3.ToDataInstances.given
import scalus.ledger.api.v3.FromDataInstances.given

import proofspace.trustregistry.common.*

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
case class TrustRegistryChange(id: TxId, registryId: TxId, operations: scalus.prelude.List[TrustRegistryOperation], time: PosixTime)

object TrustRegistryChange {
  
  given ToData[TrustRegistryChange] = ToData.deriveCaseClass[TrustRegistryChange](0)
  
  given FromData[TrustRegistryChange] = FromData.deriveCaseClass[TrustRegistryChange]

}


enum TrustRegistryOperation  {

  case AddDid(did: ByteString)
  case RemoveDid(did: ByteString)
  case ChangeMaintanceModel(newMaintanceModel: TrustRegistryMaintanceModel)
  
  
}

object TrustRegistryOperation {

  given ToData[TrustRegistryOperation] = (op:TrustRegistryOperation) => {
    op match
      case x@TrustRegistryOperation.AddDid(did) =>
        Builtins.constrData(0, scalus.builtin.List(Data.B(x.did)) )
        //Data.Constr(0, Builtins.mkCons(Data.B(x)) scala.collection.immutable.List(Data.B(x.did)))
      case x@TrustRegistryOperation.RemoveDid(did) =>
        //Data.Constr(1, scala.collection.immutable.List(Data.B(x.did)))
        Builtins.constrData(1, scalus.builtin.List(Data.B(x.did)) )
      case x@TrustRegistryOperation.ChangeMaintanceModel(newMaintanceModel) =>
        Builtins.constrData(2, scalus.builtin.List(summon[ToData[TrustRegistryMaintanceModel]](x.newMaintanceModel)))
  }

  given FromData[TrustRegistryOperation] = FromData.deriveCaseClass[TrustRegistryOperation]

}


