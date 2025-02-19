package proofspace.trustregistry.onchain

import proofspace.trustregistry.common.PreludeListData
import proofspace.trustregistry.model.{TrustRegistryChange, TrustRegistryDatum, TrustRegistryOperation}
import scalus.builtin.{ByteString, Data}
import scalus.builtin.Data.FromData
import scalus.ledger.api.v2.OutputDatum
import scalus.ledger.api.v3.*
import scalus.prelude.Maybe.Just
import scalus.prelude.{AssocMap, Maybe}
import scalus.prelude.Prelude.{*, given}

/**
 * Single maintainer trust registry.
 * The check in minting policy is done that:
 * 1. The output is signed by the maintainer
 * 2. The output contains the trust registry operations
 * 3. The output is named by the given name
 */
@scalus.Compile
object SindleMaintainer  {
  

  /**
   * Check, if the given data is a valid trust registry operations.
   */
  def verifyDatum(datum: Data): Boolean = {
    val operations = PreludeListData.listFromData[TrustRegistryOperation](datum)
    !(scalus.prelude.List.isEmpty(operations))
  }

  def targetAddressScript(pkhBytes: ByteString)(ctx: ScriptContext): Unit = {
    val pkh = new PubKeyHash(pkhBytes)
    val unused = scalus.prelude.List.findOrFail(ctx.txInfo.signatories)(_ === pkh)
  }
  
  /**
   * Check minting policy for the single-maintainer trust registry.
   */
  def mintingPolicy(pkhBytes: ByteString, registryName: ByteString)(ctx: ScriptContext): Unit = {
    val pkh = new PubKeyHash(pkhBytes)
    val txInfo = ctx.txInfo
    
    val unused = MintingPolicyElements.verifyPkh(pkh)(ctx)

    val myOutputs = MintingPolicyElements.filterMintedOutputs(ctx, registryName, (txOut, parsedDatum, ops) => true)
    
   
    if (scalus.prelude.List.isEmpty(myOutputs)) then
      throw new Exception("No outputs with the given name")


  }



}
