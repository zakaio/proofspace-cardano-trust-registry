package proofspace.trustregistry.onchain

import proofspace.trustregistry.model.{TrustRegistryDatum, TrustRegistryOperation}
import scalus.builtin.ByteString
import scalus.ledger.api.v3.{Address, Credential, ScriptContext, ScriptInfo, TxOut}
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
  def submittForApproveMintingPolicy(registryName: ByteString, changeCost: BigInt, targetCredential: Credential)(ctx: ScriptContext): Unit = {
        val myOutputs = MintingPolicyElements.filterMintedOutputs(ctx, registryName,
            (txOut, parsedDatum, ops) => {
              if (txOut.address.credential !== targetCredential) then
                throw new Exception("Output address is not the target address")
              operationsWithPayment(txOut, parsedDatum, changeCost)
            })
        if (scalus.prelude.List.isEmpty(myOutputs)) then
          throw new Exception("No minted outputs with the given name")
  }
     
     
  def approvedMintingPolicy(registryName: ByteString, submitMintingPolicyId: ByteString, pkhBytes: ByteString)(ctx: ScriptContext): Unit = {
    val unused = scalus.prelude.List.findOrFail(ctx.txInfo.signatories)(_.hash === pkhBytes)
    val myInputs = scalus.prelude.List.findOrFail(ctx.txInfo.inputs){
      txIn =>
        AssocMap.lookup(txIn.resolved.value)(submitMintingPolicyId) match
          case scalus.prelude.Maybe.Just(byNames) =>
            AssocMap.lookup(byNames)(registryName) match
              case scalus.prelude.Maybe.Just(v) => true
              case _ => false
          case _ => false
    }
  }

  def operationsWithPayment(txOut: TxOut, parsedDatum: TrustRegistryDatum, changeCost: BigInt): Boolean = {
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
    true
  }

}
