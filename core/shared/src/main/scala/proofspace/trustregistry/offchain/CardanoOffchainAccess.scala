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
trait CardanoOffchainAccess {

   //def iterateTransactionsTo(address: Address, txId: TxId, n:Int): AddressTransactionAccess
                        
   //def translateUplcToAddress(uplc: scalus.uplc.Term): Address
   
   def scriptHash(uplc: scalus.uplc.Term): scalus.builtin.ByteString
   
   def translateUplcToScriptCred(uplc: scalus.uplc.Term): scalus.ledger.api.v1.Credential.ScriptCredential =
      scalus.ledger.api.v1.Credential.ScriptCredential(scriptHash(uplc))

   def translateUplcToMintingPolicyId(uplc: scalus.uplc.Term): scalus.builtin.ByteString =
      scriptHash(uplc)

   //def translateBeth32ToAddress(bech32: String): Address
   
   //def translateAddressToBeth32(address: Address): String
   
   ///def translateAddressToByteString(address: Address): scalus.builtin.ByteString



}
