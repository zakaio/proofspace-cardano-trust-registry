package proofspace.trustregistry.onchain

import proofspace.trustregistry.common.PreludeListData
import proofspace.trustregistry.model.{TrustRegistryChange, TrustRegistryOperation}
import scalus.builtin.{ByteString, Data}
import scalus.builtin.Data.FromData
import scalus.ledger.api.v3.{PubKeyHash, ScriptContext}
import scalus.prelude.Maybe

@scalus.Compile
object SindleMaintainer  {
  

  /**
   * Check, if the given public key hash is verified by the maintainer.
   * We assume that changes in the trust registry are packet in the transaction,
   * but do not verify this.
   */
  def verifyMin(pkh: PubKeyHash)(ctx: ScriptContext) = {
       scalus.prelude.List.findOrFail(ctx.txInfo.signatories)(_ == pkh)
  }

  /**
   * Check, if the given data is a valid trust registry operations.
   */
  def verifyDatum(datum: Data): Boolean = {
    val operations = PreludeListData.listFromData[TrustRegistryOperation](datum)
    operations != scalus.prelude.List.Nil
  }


}
