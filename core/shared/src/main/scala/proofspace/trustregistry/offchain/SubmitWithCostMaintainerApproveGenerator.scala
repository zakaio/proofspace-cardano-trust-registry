package proofspace.trustregistry.offchain

import proofspace.trustregistry.onchain.{MintingPolicyElements, SubmitWithCostMaintainerApprove}
import scalus.builtin.Data.ToData
import scalus.prelude.*
import scalus.builtin.{ByteString, FromData, ToData}
import scalus.ledger.api.v1.Credential.PubKeyCredential
import scalus.ledger.api.v1.Credential.ScriptCredential
import scalus.ledger.api.v3.*
import scalus.prelude.{*, given}
import scalus.prelude.Prelude.{*, given}
import scalus.toUplc
import scalus.uplc.Term
import scalus.uplc.TermDSL.{*, given}


class SubmitWithCostMaintainerApproveGenerator(override val cardanoOfflineAccess: CardanoOffchainAccess) extends ContractGenerator {


  override def parametersDescription: Seq[ContractParameter] =
    SubmitWithCostMaintainerApproveGenerator.parametersDescription

  final val PKH_IDX = 0
  final val COST_IDX = 1


  override def generateTargetAddressScript(name: String, params: Seq[String]): Term = {
    val pkh = getPkh(params, PKH_IDX)
    val contractScriptPar = scalus.Compiler.compile((pkBytes:ByteString, ctx: ScriptContext) => {
       val pkh = new PubKeyHash(pkBytes)
       val unused = scalus.prelude.List.findOrFail(ctx.txInfo.signatories)(_ === pkh)
    }).toUplc(true)
    val contractScript = contractScriptPar $ pkh.hash
    contractScript
  }

  override def generateTargetMintingPolicy(registryName: String, contractParameters: Seq[String]): scalus.uplc.Term = {
    val pkh = getPkh(contractParameters, PKH_IDX)
    val pkhBytes = pkh.hash
    val cost = BigInt(getInteger(contractParameters, COST_IDX))
    val regName = scalus.builtin.ByteString.fromString(registryName)
    val mintingPolicyPar = scalus.Compiler.compile(SubmitWithCostMaintainerApprove.approvedMintingPolicy(_,_,_)).toUplc(true)
    val mintingPolicyFun = mintingPolicyPar $ regName $ cost $ pkhBytes
    mintingPolicyFun
  }


  override def generateSubmitMintingPolicy(name: String, contractParameters: Seq[String]): Term = {
    import ToDataInstances.given
    import FromDataInstances.given
    val regName = scalus.builtin.ByteString.fromString(name)
    val cost = BigInt(getInteger(contractParameters, COST_IDX))
    val addressCred = cardanoOfflineAccess.translateUplcToScriptCred(generateTargetAddressScript(name, contractParameters))
    val addressBytes = addressCred.hash
    val mintingPolicyPar = scalus.Compiler.compile(
      (regName: ByteString, cost: BigInt, credBytes: ByteString) =>
        val cred = new  ScriptCredential(credBytes)
        SubmitWithCostMaintainerApprove.submittForApproveMintingPolicy(regName,cost, cred)
          ).toUplc(true)
    val mintingPolicyFun = mintingPolicyPar $ regName $ cost $ addressBytes
    mintingPolicyFun
  }

  override def hasApprovalProcess: Boolean = true
  
  override def minChangeCost(contractParameters: Seq[String]): BigInt =
    BigInt(getInteger(contractParameters, COST_IDX))  

}

object SubmitWithCostMaintainerApproveGenerator {

  val parametersDescription: Seq[ContractParameter] = Seq(
    ContractParameter("maintainer", "Maintainer of the trust registry", ContractParameterType.PubKeyHash),
    ContractParameter("cost", "Cost of the transaction", ContractParameterType.Integer),
  )


}