package proofspace.trustregistry.gateways.cardano.contractTemplates

import com.bloxbean.cardano.client.plutus.spec.PlutusV3Script
import scalus.builtin.ByteString
import scalus.uplc.Program

class MintingPolicyScript(val script: Program) {

  lazy val plutusScript: PlutusV3Script =
    PlutusV3Script.builder()
      .`type`("PlutusScriptV3")
      .cborHex(script.doubleCborHex)
      .build()
      .asInstanceOf[PlutusV3Script]

  lazy val scriptHash: ByteString = ByteString.fromArray(plutusScript.getScriptHash)



}
