package proofspace.trustregistry.onchain

import proofspace.trustregistry.model.{TrustRegistryDatum, TrustRegistryOperation}
import scalus.builtin.ByteString
import scalus.ledger.api.v3.{Address, ScriptContext, TxOut}
import scalus.prelude.{*, given}
import scalus.prelude.Prelude.{*, given}


/**
 * Anybody can submit a transaction,  maintainer must approve or reject submission,
 * Reject is burning the tokens, approve is resend the transaction to the registry address with
 * the maintainer signature and datum (or referenceInput with datum)
 */
@scalus.Compile
object SubmitWithCostMaintainerApprove {


  /**
   * @param registryName - name of the trust registry
   * @param changeCost - const of the change submission
   * @param targetAddr - address of the approved change. Usually it is the address which in in 'SimpleMaintainer' contract.
   * @param ctx - context of the transaction
   */
  def submittForApproveMintingPolicy(registryName: ByteString, changeCost: BigInt, targetAddr: Address)(ctx: ScriptContext): Unit = {
        val myOutputs = MintingPolicyElements.filterMinted(ctx, registryName,
            (txOut, parsedDatum, ops) => {
              if (txOut.address !== targetAddr) then
                throw new Exception("Output address is not the target address")
              operationsFromRefWithPayment(txOut, parsedDatum, changeCost)
            })
        if (scalus.prelude.List.isEmpty(myOutputs)) then
          throw new Exception("No minted outputs with the given name")
  }
     
     
  def approvedMintingPolicy(registryName: ByteString, changeCost: BigInt)(ctx: ScriptContext): Unit = {
    val myOutputs = MintingPolicyElements.filterMinted(ctx, registryName, 
        (txOut, parsedDatum, ops) =>
          operationsFromRefWithPayment(txOut, parsedDatum, changeCost)
    )
    if (scalus.prelude.List.isEmpty(myOutputs)) then
      throw new Exception("No minted outputs with the given name")
  }

  def operationsFromRefWithPayment(txOut: TxOut, parsedDatum: TrustRegistryDatum, changeCost: BigInt): Boolean = {
    AssocMap.lookup(txOut.value)(ByteString.empty) match
      case Maybe.Just(value) =>
        AssocMap.lookup(value)(ByteString.empty) match
          case Maybe.Just(cost) =>
            if (cost < changeCost) then
              throw new Exception("Not enough cost to propose the change")
          case Maybe.Nothing =>
            throw new Exception("No cost in the output")
      case Maybe.Nothing =>
        throw new Exception("No value in the output")
    parsedDatum match
      case TrustRegistryDatum.Operations(ops) =>
        throw new Exception("Datum in minted op should be a reference")
      case _ =>
    // do nothing
    true
  }

}
