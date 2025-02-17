package proofspace.trustregistry.offchain

import proofspace.trustregistry.onchain.UsingVotingTokens
import scalus.builtin.ByteString
import scalus.ledger.api.v1.Credential
import scalus.ledger.api.v3.{Address, PubKeyHash}
import scalus.prelude.Maybe
import scalus.uplc.Term
import scalus.uplc.TermDSL.{*, given}
import scalus.toUplc

class UsingVotingTokensGenerator(override val cardanoOfflineAccess: CardanoOfflineAccess) extends ContractGenerator {

  override def parametersDescription: Seq[ContractParameter] = Seq(
    ContractParameter("votingTokens", "Voting tokens for voting for changes", ContractParameterType.String),
    ContractParameter("votingTokenAsset", "Asset of voting tokens", ContractParameterType.String),
    ContractParameter("costVotingToken", "Minimal number of voting token", ContractParameterType.Integer),
    ContractParameter("costAda", "Minimal number of ADA", ContractParameterType.Integer),
    ContractParameter("targetAddress", "Address of the trust registry", ContractParameterType.Address),
    ContractParameter("submitAddress", "Address for submitting changes", ContractParameterType.Address),
    ContractParameter("submitMintingPolicy", "Minting policy for submitting changes", ContractParameterType.String),
  )

  val VOTING_TOKENS_IDX = 0
  val VOTING_TOKEN_ASSET_IDX = 1
  val COST_VOTING_TOKEN_IDX = 2
  val COST_ADA_IDX = 3
  val TARGET_ADDRESS_IDX = 4
  val VOTING_ADDRESS_IDX = 5

  override def generateTargetAddress(name: String, params: Seq[String]): Address = {
    cardanoOfflineAccess.translateBeth32ToAddress(params(TARGET_ADDRESS_IDX))
  }

  override def generateTargetMintingPolicy(name: String, contractParameters: Seq[String]): Term = {
    val registryName = ByteString.fromString(name)
    val votingToken = ByteString.fromHex(contractParameters(VOTING_TOKENS_IDX))
    val votingTokenAsset = ByteString.fromString(contractParameters(VOTING_TOKEN_ASSET_IDX))
    val changeCostVotingToken = BigInt(contractParameters(COST_VOTING_TOKEN_IDX))
    val changeCostAda = BigInt(contractParameters(COST_ADA_IDX))
    val targetAddress = cardanoOfflineAccess.translateBeth32ToAddress(contractParameters(TARGET_ADDRESS_IDX))
    val targetCredential = targetAddress.credential
    val (targetCredBytes, targetIsScript) = targetCredential match {
      case Credential.PubKeyCredential(pubKeyHash) => (pubKeyHash.hash, false)
      case Credential.ScriptCredential(validatorHash) => (validatorHash, true)
    }
    val uplcPar = scalus.Compiler.compile({
      (registryName: ByteString,
       votingToken: ByteString,
       votingTokenAsset: ByteString,
       changeCostVotingToken: BigInt,
       changeCostAda: BigInt,
       targetCredBytes: ByteString,
       targetCredIsScript: Boolean
      ) =>
        val targetCredential =
          if targetCredIsScript then
            new Credential.ScriptCredential(targetCredBytes)
          else
            new Credential.PubKeyCredential(new PubKeyHash(targetCredBytes))
        val targetAddress = new Address(targetCredential, Maybe.Nothing)
        UsingVotingTokens.mintingPolicy(registryName, votingToken, votingTokenAsset, changeCostVotingToken, changeCostAda, targetAddress)
    }).toUplc(true)
    val fun = uplcPar $ registryName $ votingToken $ votingTokenAsset $ changeCostVotingToken $ changeCostAda $ targetCredBytes $ targetIsScript
    fun
  }

  override def generateVotingAddress(name: String, params: Seq[String]): Address = {
    cardanoOfflineAccess.translateBeth32ToAddress(params(VOTING_ADDRESS_IDX))
  }

  override def generateVotingMintingPolicy(name: String, contractParameters: Seq[String]): Term = {

  }


}
