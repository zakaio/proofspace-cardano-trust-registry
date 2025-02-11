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

object SingleMaintainerGenerator extends ContractGenerator {

  val PKH_IDX = 0

  override def parametersDescription: Seq[ContractParameter] =
    Seq(ContractParameter("maintainer", "Maintainer of the trust registry", ContractParameterType.PubKeyHash))

  override def generateTargetAddress(name: String, params:Seq[String]): Address = {
    val pkh = getPkh(params)
    val credenial = PubKeyCredential(pkh)
    val retval = Address(credenial, Maybe.Nothing)
    retval
  }

  override def generateMintingPolicy(name: String, contractParameters: Seq[String]): scalus.uplc.Term = {
    val pkhBytes = scalus.builtin.ByteString.fromHex(contractParameters(PKH_IDX))
    val mintingPolicyFun = scalus.Compiler.compile(SindleMaintainer.mintingPolicy(_,_)).toUplc(true)
    val retval = mintingPolicyFun $ pkhBytes $ scalus.builtin.ByteString.fromString(name)
    retval
  }

  override def generateVotingAddress(name: String, params: Seq[String]): Address = {
    generateTargetAddress(name, params)
  }

  def getPkh(patams:Seq[String]): PubKeyHash = {
    val pkhBytes = scalus.builtin.ByteString.fromHex(patams(PKH_IDX))
    PubKeyHash(pkhBytes)
  }

}
