package proofspace.trustregistry.offchain

import proofspace.trustregistry.onchain.{MintingPolicyElements, SubmitWithCostMaintainerApprove}
import scalus.builtin.Data.ToData
import scalus.prelude.*
import scalus.builtin.{ByteString, FromData, ToData}
import scalus.ledger.api.v1.Credential.PubKeyCredential
import scalus.ledger.api.v1.Credential.ScriptCredential
import scalus.ledger.api.v3.*
import scalus.toUplc
import scalus.uplc.Term
import scalus.uplc.TermDSL.{*, given}

class SubmitWithCostMaintainerApproveGenerator(override val cardanoOfflineAccess: CardanoOfflineAccess) extends ContractGenerator {


  override def parametersDescription: Seq[ContractParameter] = Seq(
    ContractParameter("maintainer", "Maintainer of the trust registry", ContractParameterType.PubKeyHash),
    ContractParameter("cost", "Cost of the transaction", ContractParameterType.Integer),
  )

  final val PKH_IDX = 0
  final val COST_IDX = 1

  override def generateTargetAddress(name: String, params: Seq[String]): Address = {
    val pkh = getPkh(params, PKH_IDX)
    val pkhBytes = pkh.hash
    val uplcPar = scalus.Compiler.compile( (pkh: PubKeyHash) =>
      MintingPolicyElements.verifyPkh(pkh)
    ).toUplc(true)
    val uplc = uplcPar $ pkhBytes
    val retval = cardanoOfflineAccess.translateUplcToAddress(uplc)
    retval
  }

  override def generateTargetMintingPolicy(registryName: String, contractParameters: Seq[String]): scalus.uplc.Term = {
    val pkh = getPkh(contractParameters, PKH_IDX)
    val cost = BigInt(getInteger(contractParameters, COST_IDX))
    val regName = scalus.builtin.ByteString.fromString(registryName)
    val mintingPolicyPar = scalus.Compiler.compile(SubmitWithCostMaintainerApprove.approvedMintingPolicy(_,_)).toUplc(true)
    val mintingPolicyFun = mintingPolicyPar $ regName $ cost
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
    import ToDataInstances.given
    import FromDataInstances.given
    val regName = scalus.builtin.ByteString.fromString(name)
    val cost = BigInt(getInteger(contractParameters, COST_IDX))
    val address: Address = generateVotingAddress(name, contractParameters)
    val addressCred = address.credential
    val (addressBytes, isScript) = addressCred match
      case PubKeyCredential(pkh) => (pkh.hash, false)
      case ScriptCredential(skh) => (skh, true)
    val mintingPolicyPar = scalus.Compiler.compile(
      (regName: ByteString, cost: BigInt, credBytes: ByteString, isScript: Boolean) =>
        val cred = if isScript then new  ScriptCredential(credBytes) else new PubKeyCredential( new PubKeyHash(credBytes))
        SubmitWithCostMaintainerApprove.submittForApproveMintingPolicy(regName,cost, new Address(cred,Maybe.Nothing))
          ).toUplc(true)
    val mintingPolicyFun = mintingPolicyPar $ regName $ cost $ addressBytes $ isScript
    mintingPolicyFun
  }

}
