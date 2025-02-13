package proofspace.trustregistry.offchain

import proofspace.trustregistry.onchain.{MintingPolicyElements, SubmitWithCostMaintainerApprove}
import scalus.ledger.api.v3.*
import scalus.toUplc
import scalus.uplc.Term

class SubmitWIthCostMaintainerApproveGenerator(override val cardanoOfflineAccess: CardanoOfflineAccess) extends ContractGenerator {


  override def parametersDescription: Seq[ContractParameter] = Seq(
    ContractParameter("maintainer", "Maintainer of the trust registry", ContractParameterType.PubKeyHash),
    ContractParameter("cost", "Cost of the transaction", ContractParameterType.Integer),
  )

  final val PKH_IDX = 0
  final val COST_IDX = 1

  override def generateTargetAddress(name: String, params: Seq[String]): Address = {
    val pkh = getPkh(params, PKH_IDX)
    val pkhBytes = pkh.hash
    val uplc = scalus.Compiler.compile(
        .flatMap(_ => MintingPolicyElements.verifyPkh(new PubKeyHash(pkhBytes))(_))
      //MintingPolicyElements.verifyPkh(new PubKeyHash(pkhBytes))(_)
    ).toUplc(true)
    val retval = cardanoOfflineAccess.translateUplcToAddress(uplc)
    retval
  }

  override def generateTargetMintingPolicy(registryName: String, contractParameters: Seq[String]): scalus.uplc.Term = {
    val pkh = getPkh(contractParameters, PKH_IDX)
    val cost = BigInt(getInteger(contractParameters, COST_IDX))
    val regName = scalus.builtin.ByteString.fromString(registryName)
    val mintingPolicyFun = scalus.Compiler.compile(SubmitWithCostMaintainerApprove.approvedMintingPolicy(regName,cost)).toUplc(true)
    mintingPolicyFun
  }

  /**
   * It can be the same address as the target address
   * @param name
   * @param params
   * @return
   */
  override def generateVotingAddress(name: String, params: Seq[String]): Address = {
    generateTargetAddress(name, params)
  }

  override def generateVotingMintingPolicy(name: String, contractParameters: Seq[String]): Term = {
    val regName = scalus.builtin.ByteString.fromString(name)
    val cost = BigInt(getInteger(contractParameters, COST_IDX))
    val address = generateVotingAddress(name, contractParameters)
    val mintingPolicyFun = scalus.Compiler.compile(
      SubmitWithCostMaintainerApprove.submittForApproveMintingPolicy(regName,cost,address)).toUplc(true)
    mintingPolicyFun
  }

}
