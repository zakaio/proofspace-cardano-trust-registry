package proofspace.trustregistry.onchain

import scalus.builtin.ByteString
import scalus.ledger.api.v1.Credential
import scalus.ledger.api.v3.{Address, ScriptContext, TxOut}
import scalus.prelude.{AssocMap, Maybe}
import scalus.prelude.Prelude.{*, given}


/**
 * Anybody can submit a change, which is supplement with voting tokens.
 */
@scalus.Compile
object UsingVotingTokens {


    def  mintingPolicy(registryName: ByteString,
                       votingToken: ByteString,
                       votingTokenAsset: ByteString,
                       changeCostVotingToken: BigInt,
                       changeCostAda: BigInt,
                       targetCredential: Credential,
                       submitMintingPolicyId: ByteString
                      )(ctx: ScriptContext): Unit = {
       val myInputs = scalus.prelude.List.filter(ctx.txInfo.inputs){
         txIn =>
           val resolved = txIn.resolved
           if (txIn.resolved.address.credential !== targetCredential) then
             false
           else
             AssocMap.lookup(resolved.value)(submitMintingPolicyId) match
                case Maybe.Just(byNames) =>
                  AssocMap.lookup(byNames)(registryName) match
                    case Maybe.Just(v) => true
                    case _ =>
                      throw new Exception("Minted outputs with the other name as expected")
                case _ => false
       }
        if (scalus.prelude.List.isEmpty(myInputs)) then
          throw new Exception("No minted outputs with the given name")
       val votingInputs = scalus.prelude.List.foldLeft(ctx.txInfo.inputs, BigInt(0)) { (acc, txIn) =>
         AssocMap.lookup(txIn.resolved.value)(votingToken) match
           case Maybe.Just(assets) =>
             AssocMap.lookup(assets)(votingTokenAsset) match
               case Maybe.Just(votingTokenAmount) =>
                 if (txIn.resolved.address.credential === targetCredential) then
                    acc + votingTokenAmount
                 else
                   acc
               case Maybe.Nothing =>
                 acc
           case Maybe.Nothing =>
             acc
       }
       if (votingInputs < changeCostVotingToken) then
         throw new Exception("Not enough voting tokens to propose the change")
       val changeCostInputs = scalus.prelude.List.foldLeft(ctx.txInfo.inputs, BigInt(0)) { (acc, txIn) =>
         AssocMap.lookup(txIn.resolved.value)(ByteString.empty) match
           case Maybe.Just(assets) =>
             AssocMap.lookup(assets)(ByteString.empty) match
               case Maybe.Just(adaAmount) =>
                 if (txIn.resolved.address.credential === targetCredential) then
                   acc + adaAmount
                 else
                   acc
               case Maybe.Nothing =>
                 acc
           case Maybe.Nothing =>
             acc
       }
       if (changeCostInputs < changeCostAda) then
          throw new Exception("Not enough ADA to propose the change")
    }


}
