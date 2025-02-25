package proofspace.trustregistry.fakes


import scala.concurrent.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map as MutableMap
import scala.util.*
import scalus.ledger.api.v3.{Address, PosixTime, TxId, TxInfo}
import cps.*
import cps.monads.{*, given}
import cps.stream.*
import proofspace.trustregistry.offchain.*
import scalus.builtin.ByteString
import scalus.flat
import scalus.flat.Flat
import scalus.uplc.{*, given}
import scalus.uplc.FlatInstantces.{*,given}

import java.util.concurrent.atomic.AtomicBoolean

class FakeCardanoAccess extends CardanoOffchainAccess {

  val transactionsByTime: ArrayBuffer[(TxInfo,PosixTime)] = ArrayBuffer.empty
  val transactionsByAddress: MutableMap[Address, ArrayBuffer[TxInfoAsOfflineAccess]] = MutableMap.empty
  val listeners: ArrayBuffer[(TxInfo, PosixTime) => Unit] = ArrayBuffer.empty

  // incorrect, but tow
  override def scriptHash(uplc: Term): ByteString =
    val bitSize = summon[Flat[Term]].bitSize(uplc)
    val encoderState = new flat.EncoderState(bitSize/8 + 1)
    scalus.flat.encode(uplc, encoderState)
    val bytes = encoderState.buffer
    val retval = scalus.utils.Utils.sha2_256( bytes )
    scalus.builtin.ByteString.fromArray(retval)



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



  def iterateTransactionsTo(address: Address, txId: TxId, n: Int): AddressTransactionAccess = {

    val txs = transactionsByAddress.getOrElse(address, ArrayBuffer.empty)
    val start = txs.indexWhere(_.id == txId)
    val end = start + n

    val updatedFlag = new AtomicBoolean(false)
    val transactions = asyncStream[AsyncList[Future, CardanoTransactionOfflineAccess]] { out =>
      for(i <- start until end) {
        val tx = synchronized(txs(i))
        out.emit(tx)
      }
      updatedFlag.set(true)

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

    AddressTransactionAccess(address, transactions, updatedFlag)

  }



}
