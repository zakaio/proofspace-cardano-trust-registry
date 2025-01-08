package proofspace.trustregistry.offchain

import scala.concurrent.Future
import scalus.ledger.api.v3.{Address, PubKeyHash, TxId}
import cps.stream.*

import java.util.concurrent.atomic.AtomicBoolean

case class AddressTransactionAccess(
  address: Address,
  transactions: AsyncList[Future, CardanoTransactionOfflineAccess],
  updateFlag: AtomicBoolean
                                   )

/**
 * Encapsulate offline access to cardano,
 * have different implementation for jvm and js.
 */
trait CardanoOfflineAccess {

   def iterateTransactionsTo(address: Address, txId: TxId, n:Int): AddressTransactionAccess
                        


}
