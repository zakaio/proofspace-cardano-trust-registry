package proofspace.trustregistry.offchain

import proofspace.trustregistry.onchain.{SindleMaintainer, SubmitWithCostMaintainerApprove, UsingVotingTokens}
import scalus.builtin.ByteString
import scalus.ledger.api.v3.Credential
import scalus.ledger.api.v1.Credential.PubKeyCredential
import scalus.ledger.api.v1.Credential.ScriptCredential
import scalus.ledger.api.v3.{Address, PubKeyHash}
import scalus.prelude.Maybe
import scalus.uplc.Term
import scalus.uplc.TermDSL.{*, given}
import scalus.toUplc

class UsingVotingTokensGenerator(override val cardanoOfflineAccess: CardanoOffchainAccess) extends ContractGenerator {

  override def parametersDescription: Seq[ContractParameter] = Seq(
    ContractParameter("votingTokens", "Voting tokens for voting for changes", ContractParameterType.String),
    ContractParameter("votingTokenAsset", "Asset of voting tokens", ContractParameterType.String),
    ContractParameter("costVotingToken", "Minimal number of voting token", ContractParameterType.Integer),
    ContractParameter("costAda", "Minimal number of ADA", ContractParameterType.Integer),
    ContractParameter("targetPkh", "pubkeyhash of target address", ContractParameterType.Address),
  )

  val VOTING_TOKENS_IDX = 0
  val VOTING_TOKEN_ASSET_IDX = 1
  val COST_VOTING_TOKEN_IDX = 2
  val COST_ADA_IDX = 3
  val PKH_IDX = 4

  override def generateTargetAddressScript(name: String, params: Seq[String]): Term = {
    val pkhBytes = scalus.builtin.ByteString.fromHex(params(PKH_IDX))
    val contractScriptPar = scalus.Compiler.compile(SindleMaintainer.targetAddressScript(_)).toUplc(true)
    val contractScript = contractScriptPar $ pkhBytes
    contractScript
  }

  override def generateTargetMintingPolicy(name: String, contractParameters: Seq[String]): Term = {
    val registryName = ByteString.fromString(name)
    val votingToken = ByteString.fromHex(contractParameters(VOTING_TOKENS_IDX))
    val votingTokenAsset = ByteString.fromString(contractParameters(VOTING_TOKEN_ASSET_IDX))
    val changeCostVotingToken = BigInt(contractParameters(COST_VOTING_TOKEN_IDX))
    val changeCostAda = BigInt(contractParameters(COST_ADA_IDX))
    val targetCredential = cardanoOfflineAccess.translateUplcToScriptCred(generateTargetAddressScript(name, contractParameters))
    val targetCredBytes = targetCredential.hash
    val submitMintingPolicy = generateSubmitMintingPolicy(name, contractParameters)
    val submitMintingPolicyId = cardanoOfflineAccess.translateUplcToMintingPolicyId(submitMintingPolicy)
    val uplcPar = scalus.Compiler.compile({
      (registryName: ByteString,
       votingToken: ByteString,
       votingTokenAsset: ByteString,
       changeCostVotingToken: BigInt,
       changeCostAda: BigInt,
       targetCredBytes: ByteString,
       submitMintingPolicyId: ByteString
      ) =>
        val targetCredential = new Credential.ScriptCredential(targetCredBytes)
        UsingVotingTokens.mintingPolicy(registryName, votingToken, votingTokenAsset,
          changeCostVotingToken, changeCostAda, targetCredential, submitMintingPolicyId)
    }).toUplc(true)
    val fun = uplcPar $ registryName $ votingToken $ votingTokenAsset $ changeCostVotingToken $ changeCostAda $ targetCredBytes $ submitMintingPolicyId
    fun
  }


  // the same as maintainer,  anybody can submit a change.
  override def generateSubmitMintingPolicy(name: String, contractParameters: Seq[String]): Term = {
    val regName = scalus.builtin.ByteString.fromString(name)
    val cost = BigInt(getInteger(contractParameters, COST_ADA_IDX))
    val addressCred = cardanoOfflineAccess.translateUplcToScriptCred(generateTargetAddressScript(name, contractParameters))
    val addressCredBytes = addressCred.hash
    val uplcPar = scalus.Compiler.compile(
      (regName: ByteString, cost: BigInt, credBytes: ByteString) =>
        val cred = new  ScriptCredential(credBytes)
        SubmitWithCostMaintainerApprove.submittForApproveMintingPolicy(regName,cost, cred)
    ).toUplc(true)
    val uplc = uplcPar $ regName $ cost $ addressCredBytes
    uplc
  }

  override def hasApprovalProcess: Boolean = true

  override def minChangeCost(contractParameters: Seq[String]): BigInt =
    BigInt(getInteger(contractParameters, COST_ADA_IDX))

}
