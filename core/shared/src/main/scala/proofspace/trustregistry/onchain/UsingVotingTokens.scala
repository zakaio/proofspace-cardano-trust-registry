package proofspace.trustregistry.onchain

import scalus.builtin.ByteString
import scalus.ledger.api.v3.{Address, ScriptContext, TxOut}
import scalus.prelude.{AssocMap, Maybe}
import scalus.prelude.Prelude.{*, given}


/**
 * Anybody can submit a change, which is supplement with voting tokens.
 */
class UsingVotingTokens {


    def  mintingPolicy(registryName: ByteString,
                       votingToken: ByteString,
                       votingTokenAsset: ByteString,
                       changeCostVotingToken: BigInt,
                       changeCostAda: BigInt,
                       targetAddress:Address)(ctx: ScriptContext): Unit = {
       val myOutputs = MintingPolicyElements.filterMinted(ctx, registryName,
           (txOut, parsedDatum, ops) =>
             checkTxOutput(txOut, votingToken, votingTokenAsset, changeCostVotingToken, changeCostAda, targetAddress)
             true
       )
    }

    def checkTxOutput(txOut: TxOut,
                      votingToken: ByteString,
                      votingTokenAsset: ByteString,
                      changeCostVotingToken: BigInt,
                      changeCostAda: BigInt,
                      targetAddress: Address): Unit = {
      AssocMap.lookup(txOut.value)(votingToken) match
        case Maybe.Just(assets) =>
          AssocMap.lookup(assets)(votingTokenAsset) match
            case Maybe.Just(votingTokenAmount) =>
              if (votingTokenAmount < changeCostVotingToken) then
                throw new Exception("Not enough voting tokens to propose the change")
            case Maybe.Nothing =>
              throw new Exception("No voting tokens in the output")
        case Maybe.Nothing =>
          throw new Exception("No value in the output")
      AssocMap.lookup(txOut.value)(ByteString.empty) match
        case Maybe.Just(assets) =>
          AssocMap.lookup(assets)(ByteString.empty) match
            case Maybe.Just(adaAmount) =>
              if (adaAmount < changeCostAda) then
                throw new Exception("Not enough ADA to propose the change")
            case Maybe.Nothing =>
              throw new Exception("No ADA in the output")
        case Maybe.Nothing =>
          throw new Exception("No value in the output")
      if (txOut.address !== targetAddress) then
        throw new Exception("Output address is not the target address")
    }


}
