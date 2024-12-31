package proofspace.trustregistry.offchain

import scala.concurrent.Future
import scalus.ledger.api.v3.{Address, PubKeyHash, TxId}
import cps.stream.*

/**
 * Encapsulate offline access to cardano,
 * have different implementation for jvm and js.
 */
trait CardanoOfflineAccess {

   def iterateTransactionsFrom(address: Address, txId: TxId, n:Int): AsyncList[Future, CardanoTransactionOfflineAccess]


}
