package proofspace.trustregistry.offchain

import proofspace.trustregistry.model.{TrustRegistry, TrustRegistryChange, TrustRegistryOperation, TrustRegistrySnapshot}
import scalus.builtin.ByteString
import scalus.ledger.api.v3.*

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.concurrent.TrieMap
import scala.collection.immutable.Map
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cps.*
import cps.stream.*
import scalus.ledger.api.v2.OutputDatum


import scala.util.boundary
import scala.util.boundary.*
import scala.util.control.NonFatal
import scala.util.*

/**
 * Contàins the trust registry, which is maintained by the single maintainer.
 * For now all changes are stored in memory. Later will switch to db repeesentation.
 */
case class InMemoryTrustRegistrySnapshot(pkh: PubKeyHash,
                                    address:  Address,
                                    txId: TxId,
                                    override val lastTxId: TxId,
                                    override val lastTouch: PosixTime,
                                    override val name: ByteString,
                                    val dids: Map[ByteString,ByteString] = Map.empty
                                   ) extends TrustRegistrySnapshot {

  // this version keep list of dids in memory

  def id: TxId = txId

  def check(did: String): Boolean = dids.get(ByteString.fromString(did)).isDefined

  def applyChange(change: TrustRegistryChange): InMemoryTrustRegistrySnapshot = {
    val (nextName, nextDids) = applyOperations(change.operations)
    copy(name=nextName, lastTxId = change.id, lastTouch = change.time, dids = nextDids)
  }

  def applyOperations(ops: Seq[TrustRegistryOperation]): (ByteString, Map[ByteString,ByteString]) = {
    ops.foldLeft((name, dids) ) { (acc, x) =>
      x match {
        case TrustRegistryOperation.AddDids(dids) =>
          val newMap =  scalus.prelude.List.foldLeft(dids,acc._2) { (s, did) =>
            s.updated(did, did)
          }
          (name, newMap)
        case TrustRegistryOperation.RemoveDids(did) =>
          val newMap = scalus.prelude.List.foldLeft(did, acc._2) { (s, did) =>
            s.removed(did)
          }
          (name, newMap)
        case TrustRegistryOperation.ChangeName(name) =>
          (name, acc._2)
      }
    }
  }



}

class InMemoryTrustRegistry(pkh: PubKeyHash,
                            targetAddress: Address,
                            inSnapshot: InMemoryTrustRegistrySnapshot,
                            transactions: AsyncList[Future, CardanoTransactionOfflineAccess],
                            updatedFlag: AtomicBoolean
                                   ) extends TrustRegistry {

  val snapshotRef = AtomicReference(inSnapshot)

  def name = snapshotRef.get().name

  def isUpdated = updatedFlag.get()

  val registryId = inSnapshot.txId

  val foldFuture = transactions.fold(inSnapshot) { (snapshot, tx) =>
    retrieveChange(tx) match
      case Some(change) =>
        if (change.registryId == registryId  ) then
          try
            val next = snapshot.applyChange(change)
            snapshotRef.set(next)
            next
          catch
            case NonFatal(e) =>
              scribe.error(s"Failed to apply change from transaction ${tx.id}, skip", e)
              snapshot
        else snapshot      
      case None =>
        snapshot
  }
  foldFuture.onComplete{
    case Success(value) =>
      scribe.info(s"Trust registry ${inSnapshot.name} is up to date")
    case Failure(e) =>
      scribe.error(s"Failed to update trust registry ${inSnapshot.name}", e)
  }

  def snapshot: TrustRegistrySnapshot = snapshotRef.get()

  def retrieveChange(tx: CardanoTransactionOfflineAccess): Option[TrustRegistryChange] = {
    val signed = tx.signatories.contains(pkh)
    val operations = boundary[Seq[TrustRegistryOperation]] {
       if (!signed) then
         scribe.warn(s"Transaction ${tx.id} is not signed by the maintainer, skipping")
         break(Seq.empty)
       //val data = datumHashes.flatMap(tx.data.get)
       tx.outputs.filter(_.address == targetAddress).flatMap{ out =>
         out.datum match
           case OutputDatum.NoOutputDatum => Seq.empty
           case OutputDatum.OutputDatum(datum) =>
             try
               val change = datum.to[TrustRegistryOperation]
               Seq(change)
             catch
               case NonFatal(e) =>
                 scribe.error(s"Failed to parse trust registry change from datum ${datum}", e)
                 break(Seq.empty)
           case OutputDatum.OutputDatumHash(dh) =>
                 tx.data.get(dh) match
                   case Some(datum) =>
                     try
                       val change = datum.to[TrustRegistryOperation]
                       Seq(change)
                     catch
                       case NonFatal(e) =>
                         scribe.error(s"Failed to parse trust registry change from datum ${datum}", e)
                         break(Seq.empty)
                   case None =>
                     scribe.error(s"Trust registry change is not inlined in the transaction, skipping")
                     break(Seq.empty)
       }
    }
    if (operations.isEmpty) then
      None
    else
      Some(TrustRegistryChange(tx.id, snapshotRef.get().id, operations, tx.time))
  }

}

