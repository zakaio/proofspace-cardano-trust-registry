package proofspace.trustregistry.fakes


import scala.concurrent.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map as MutableMap
import scala.util.*
import scalus.ledger.api.v3.{Address, TxId, TxInfo, PosixTime}
import cps.*
import cps.monads.{*, given}
import cps.stream.*
import proofspace.trustregistry.offchain.*

class FakeCardanoAccess extends CardanoOfflineAccess {

  val transactionsByTime: ArrayBuffer[(TxInfo,PosixTime)] = ArrayBuffer.empty
  val transactionsByAddress: MutableMap[Address, ArrayBuffer[TxInfoAsOfflineAccess]] = MutableMap.empty
  val listeners: ArrayBuffer[(TxInfo, PosixTime) => Unit] = ArrayBuffer.empty

  def putTransaction(tx: TxInfo, time: PosixTime ): Unit = {
    this.synchronized{
      println(s"putting transaction: $tx")
      val addresses = tx.outputs.toList.map(_.address)
      for(a <- addresses) {
        val txs = transactionsByAddress.getOrElseUpdate(a, ArrayBuffer.empty)
        txs += TxInfoAsOfflineAccess(tx, time)
      }
      transactionsByTime += ((tx, time))
      listeners.foreach(_(tx, time))
    }
  }

  override def iterateTransactionsFrom(address: Address, txId: TxId, n: Int): AsyncList[Future, CardanoTransactionOfflineAccess] = {

    val txs = transactionsByAddress.getOrElse(address, ArrayBuffer.empty)
    val start = txs.indexWhere(_.id == txId)
    val end = start + n

    val retval = asyncStream[AsyncList[Future, CardanoTransactionOfflineAccess]] { out =>
      for(i <- start until end) {
        out.emit(txs(i))
      }
      // TODO: mark the registry as realtime (not historical)
      listeners.append {
        (tx: TxInfo, time) => out.emitAsync(TxInfoAsOfflineAccess(tx,time)).onComplete {
          case Success(_) =>
          case Failure(e) => 
            println(s"error in emitting tx: $e")
            e.printStackTrace()
        }
      }
    }

    retval

  }


}
