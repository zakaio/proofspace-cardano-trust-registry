package proofspace.trustregistry.offchain

import proofspace.trustregistry.model.{TrustRegistryChange, TrustRegistryMaintanceModel, TrustRegistryOperation, TrustRegistrySnapshot}
import scalus.builtin.ByteString
import scalus.ledger.api.v3.*

import scala.collection.concurrent.TrieMap
import scala.collection.immutable.Map

/**
 * Contàins the trust registry, which is maintained by the single maintainer.
 * For now all changes are stored in memory. Later will switch to db repeesentation.
 */
case class SingleMaintainerTrustRegistry(pkh: PubKeyHash,
                                    address:  Address,
                                    txId: TxId,
                                    override val lastTxId: TxId,
                                    override val lastTouch: PosixTime,
                                    override val name: String,
                                    val dids: Map[ByteString,ByteString] = Map.empty
                                   ) extends TrustRegistrySnapshot {

  // this version keep list of dids in memory
  
  def id: TxId = txId
  
  def check(did: String): Boolean = dids.get(ByteString.fromString(did)).isDefined

  def applyChange(change: TrustRegistryChange): TrustRegistrySnapshot = {
    val nextDids = applyOperations(change.operations)
    copy(lastTxId = change.id, lastTouch = change.time, dids = nextDids)
  }
  
  def applyOperations(ops: scalus.prelude.List[TrustRegistryOperation]): Map[ByteString,ByteString] = {
    scalus.prelude.List.foldLeft(ops, dids ) { (acc, x) =>
      x match {
        case TrustRegistryOperation.AddDid(did) =>
          acc.updated(did, did)
        case TrustRegistryOperation.RemoveDid(did) =>
          acc.removed(did)
        case TrustRegistryOperation.ChangeMaintanceModel(newMaintanceModel) =>
          throw new IllegalArgumentException("ChangeMaintanceModel is not supported")
      }
    }
  }

  val maintanceModel = TrustRegistryMaintanceModel.SingleMaintainer(pkh, address, BigInt(0))


}
