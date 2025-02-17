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

class UsingVotingTokensGenerator(override val cardanoOfflineAccess: CardanoOfflineAccess) extends ContractGenerator {

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
    val targetAddress = cardanoOfflineAccess.translateUplcToAddress(generateTargetAddressScript(name, contractParameters))
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


  // the same as maintainer,  anybody can submit a change.
  override def generateSubmitMintingPolicy(name: String, contractParameters: Seq[String]): Term = {
    val regName = scalus.builtin.ByteString.fromString(name)
    val cost = BigInt(getInteger(contractParameters, COST_ADA_IDX))
    val address: Address = cardanoOfflineAccess.translateUplcToAddress(generateTargetAddressScript(name, contractParameters))
    val addressCred = address.credential
    val (addressBytes, isScript) = addressCred match
      case PubKeyCredential(pkh) => (pkh.hash, false)
      case ScriptCredential(skh) => (skh, true)
    val uplcPar = scalus.Compiler.compile(
      (regName: ByteString, cost: BigInt, credBytes: ByteString, isScript: Boolean) =>
        val cred = if isScript then new  ScriptCredential(credBytes) else new PubKeyCredential( new PubKeyHash(credBytes))
        SubmitWithCostMaintainerApprove.submittForApproveMintingPolicy(regName,cost, new Address(cred,Maybe.Nothing))
    ).toUplc(true)
    val uplc = uplcPar $ regName $ cost $ addressBytes $ isScript
    uplc
  }

  override def minChangeCost(contractParameters: Seq[String]): BigInt =
    BigInt(getInteger(contractParameters, COST_ADA_IDX))

}
