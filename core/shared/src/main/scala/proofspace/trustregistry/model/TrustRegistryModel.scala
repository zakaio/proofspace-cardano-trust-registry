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
 * Change records are situated in the
 */
case class TrustRegistryChange(id: TxId, registryId: TxId, operations: Seq[TrustRegistryOperation], time: PosixTime)


enum TrustRegistryOperation  {

  case AddDids(dids: scalus.prelude.List[ByteString])
  case RemoveDids(dids: scalus.prelude.List[ByteString])
  case SetName(name: ByteString)

}

@scalus.Compile
object TrustRegistryOperation {

  given ToData[TrustRegistryOperation] = (op:TrustRegistryOperation) => {
    op match
      case TrustRegistryOperation.AddDids(dids) =>
        val dd = PreludeListData.listToData(dids)
        Builtins.constrData(0, scalus.builtin.List(dd) )
      case TrustRegistryOperation.RemoveDids(did) =>
        val dd = PreludeListData.listToData(did)
        Builtins.constrData(1, scalus.builtin.List(dd) )
      case TrustRegistryOperation.SetName(name) =>
        val dn: Data = Data.B(name)
        Builtins.constrData(2, scalus.builtin.List(dn))
  }

  given FromData[TrustRegistryOperation] = FromData.deriveCaseClass[TrustRegistryOperation]

  
  given listOperationsToData: ToData[scalus.prelude.List[TrustRegistryOperation]] = (ops: scalus.prelude.List[TrustRegistryOperation]) => {
    PreludeListData.listToData(ops)
  }
  
  given listOperationsFromData: FromData[scalus.prelude.List[TrustRegistryOperation]] = (data: scalus.builtin.Data) => {
    PreludeListData.listFromData(data)
  }
  
}

enum TrustRegistryDatum {
  case Operations(ops: scalus.prelude.List[TrustRegistryOperation])
  case SeeReferenceIndex(index: BigInt)
  case SeeNormalInput(index: BigInt)
}

@scalus.Compile
object TrustRegistryDatum {

  given ToData[TrustRegistryDatum] = (datum: TrustRegistryDatum) => {
    datum match
      case TrustRegistryDatum.Operations(ops) =>
        val opsData = TrustRegistryOperation.listOperationsToData(ops)
        Builtins.constrData(0, scalus.builtin.List(opsData))
      case TrustRegistryDatum.SeeReferenceIndex(index) =>
        val idxData: Data = Data.I(index)
        Builtins.constrData(1, scalus.builtin.List(idxData))
      case TrustRegistryDatum.SeeNormalInput(index) =>
        val idxData: Data = Data.I(index)
        Builtins.constrData(2, scalus.builtin.List(idxData))

  }

  given FromData[TrustRegistryDatum] = FromData.deriveCaseClass[TrustRegistryDatum]


}



