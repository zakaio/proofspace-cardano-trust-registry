package proofspace.trustregistry.model

import scalus.builtin.{List => _, *}
import scalus.builtin.Data.{FromData, ToData}
import scalus.builtin.ToDataInstances.given
import scalus.builtin.FromDataInstances.given
import scalus.ledger.api.v3.{*, given}
import scalus.ledger.api.v3.ToDataInstances.given
import scalus.ledger.api.v3.FromDataInstances.given

import proofspace.trustregistry.common.*

/**
 * Trust registry is defined by transactions, which is payed from smart-contract
 * address (to any address, verified by the smart contract).
 *
 * Transaction Inputs (payments to smart-contract) can be used as reference inputs
 * in the transactions from the trust-registry smart-contract.
 *
 * Trus-tregistry is restored by the all utxos, which are payed from the smart-contract.
 */


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
  def name: ByteString

  /**
   * Check, if the given DID is in the trust registry.
   */
  def check(did: String): Boolean

  /**
   * Apply change to the trust registry and return new snapshot.
   */
  def applyChange(change: TrustRegistryChange): TrustRegistrySnapshot


  /**
   * Address where the trust registry was created.
   * All incomint transactiosn from this adress is considered
   * to be changes of the trust registry.
   *
   * Usually on change submit address is smart contract, which
   * check the integrity of the trust registry and correctness of the changes.
   */
  def address: Address


}

/**
 * Trust registry, which is changed with time.
 */
trait TrustRegistry {

  def name: ByteString
  
  def isUpdated: Boolean 

  def snapshot: TrustRegistrySnapshot

}


/**
 * Change describe by the set of the trust registry operation.
 * Mapping from transaction to changed is defined by maintance model.
 */
case class TrustRegistryChange(id: TxId, registryId: TxId, operations: Seq[TrustRegistryOperation], time: PosixTime)


enum TrustRegistryOperation  {

  case AddDid(did: ByteString)
  case RemoveDid(did: ByteString)
  case ChangeName(name: ByteString)

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
      case x@TrustRegistryOperation.ChangeName(name: ByteString) =>
        //Data.Constr(2, scala.collection.immutable.List(Data.B(x.name)))
        Builtins.constrData(2, scalus.builtin.List(Data.B(x.name)) )
  }

  given FromData[TrustRegistryOperation] = FromData.deriveCaseClass[TrustRegistryOperation]


}


sealed trait TrustRegistryChangeRecord
case class InlineTrustRegistryStart(name: ByteString) extends TrustRegistryChangeRecord
case class InlineChangeRecord(operations: scalus.prelude.List[TrustRegistryOperation]) extends TrustRegistryChangeRecord
case class ReferenceChangeRecord(referenceInput: BigInt) extends TrustRegistryChangeRecord




object TrustRegistryChangeRecord {


  implicit def xtoData: ToData[TrustRegistryChangeRecord] = (r: TrustRegistryChangeRecord) => {
    r match
      case x@InlineTrustRegistryStart(name) =>
        Builtins.constrData(0, scalus.builtin.List(Data.B(x.name)) )
      case x@InlineChangeRecord(operations) =>
        Builtins.constrData(1, scalus.builtin.List(PreludeListData.listToData(operations)) )
      case x@ReferenceChangeRecord(referenceInput) =>
        Builtins.constrData(2, scalus.builtin.List(Builtins.iData(referenceInput)))
  }


  // error in the compiler
  //implicit def xfromData: FromData[TrustRegistryChangeRecord] = FromData.deriveCaseClass[TrustRegistryChangeRecord]




}
