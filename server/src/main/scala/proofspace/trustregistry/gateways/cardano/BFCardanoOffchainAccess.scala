package proofspace.trustregistry.gateways.cardano

import proofspace.trustregistry.offchain.CardanoOffchainAccess
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService
import com.bloxbean.cardano.client.plutus.spec.PlutusV3Script
import scalus.builtin.ByteString
import scalus.ledger.api.v3.Address
import scalus.uplc.Term
import scalus.*

class BFCardanoOffchainAccess(bfService: BFBackendService) extends CardanoOffchainAccess {

  override def scriptHash(uplc: Term): ByteString = {
    val targetCborHex = uplc.plutusV3.doubleCborHex
    val script = PlutusV3Script.builder().cborHex(targetCborHex).build()
    ByteString.fromArray(script.getScriptHash)
  }

}
