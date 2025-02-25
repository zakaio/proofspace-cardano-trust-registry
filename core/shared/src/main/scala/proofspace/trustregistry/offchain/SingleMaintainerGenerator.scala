package proofspace.trustregistry.offchain

import proofspace.trustregistry.onchain.SindleMaintainer
import scalus.builtin.ByteString
import scalus.ledger.api.v1.Credential
import scalus.ledger.api.v1.Credential.PubKeyCredential
import scalus.ledger.api.v3.{Address, PubKeyHash}
import scalus.prelude.Maybe
import scalus.toUplc
import scalus.uplc.Term
import scalus.uplc.TermDSL.{*, given}

import scala.language.implicitConversions

class SingleMaintainerGenerator(override val cardanoOfflineAccess: CardanoOffchainAccess) extends ContractGenerator {

  val PKH_IDX = 0

  override def parametersDescription: Seq[ContractParameter] =
    SingleMaintainerGenerator.parametersDescription
  
  override def generateTargetAddressScript(name: String, params: Seq[String]): scalus.uplc.Term = {
    val pkhBytes = scalus.builtin.ByteString.fromHex(params(PKH_IDX))
    val contractScriptPar = scalus.Compiler.compile(SindleMaintainer.targetAddressScript(_)).toUplc(true)
    val contractScript = contractScriptPar $ pkhBytes
    contractScript
  }
  
  override def generateTargetMintingPolicy(name: String, contractParameters: Seq[String]): scalus.uplc.Term = {
    val pkhBytes = scalus.builtin.ByteString.fromHex(contractParameters(PKH_IDX))
    val mintingPolicyFun = scalus.Compiler.compile(SindleMaintainer.mintingPolicy(_,_)).toUplc(true)
    val retval = mintingPolicyFun $ pkhBytes $ scalus.builtin.ByteString.fromString(name)
    retval
  }
  
  override def hasApprovalProcess: Boolean = false

  override def generateSubmitMintingPolicy(name: String, contractParameters: Seq[String]): Term = {
    generateTargetMintingPolicy(name, contractParameters)
  }

  override def minChangeCost(contractParameters: Seq[String]): BigInt = BigInt(0)


}

object SingleMaintainerGenerator {

  def parametersDescription: Seq[ContractParameter] =
    Seq(ContractParameter("maintainer", "Maintainer of the trust registry", ContractParameterType.PubKeyHash))

}