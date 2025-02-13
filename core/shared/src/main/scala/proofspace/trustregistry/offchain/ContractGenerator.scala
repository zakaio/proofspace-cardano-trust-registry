package proofspace.trustregistry.offchain

import scalus.ledger.api.v3.{Address, PubKeyHash}

enum ContractParameterType {
  case Integer
  case String
  case Address
  case PubKeyHash
  case Bool
  case Bytes
}

case class ContractParameter(name: String, description: String, tp: ContractParameterType)

trait ContractGenerator {

  def parametersDescription: Seq[ContractParameter]

  /**
   * Addresses, that will be used for trust registry. Transactions to this address
   * will be used to update trust registry.
   * @param name
   * @return
   */
  def generateTargetAddress(name: String, params: Seq[String]): Address

  /**
   * Addresses, that will be used for voting for changes.
   * Changes, which will need to be accepted or rejected sends to this address.
   * @param pkh
   * @param name
   * @return
   */
  def generateVotingAddress(name: String, params: Seq[String]): Address

  /**
   * Minting policy for transactions, from wich we restore the trust registry.
   * @param name
   * @param contractParameters
   * @return
   */
  def generateTargetMintingPolicy(name: String,
                                  contractParameters: Seq[String],
                                 ): scalus.uplc.Term


  /**
   * Generate minting policy for transactions, which submit changes to the trust registry.
   * If we have a one-step submission (i.e. transaction is sended with the voting tokens, necessary for accepting)
   * then VotingMintingPolicy is the same as TargetMintingPolicy.
   * @param pkh
   * @param name
   * @param contractParameters
   * @return
   */
  def generateVotingMintingPolicy(name: String,
                            contractParameters: Seq[String],
                           ): scalus.uplc.Term


  protected def cardanoOfflineAccess: CardanoOfflineAccess

  protected def getPkh(params:Seq[String], idx:Int): PubKeyHash = {
    val pkhBytes = scalus.builtin.ByteString.fromHex(params(idx))
    PubKeyHash(pkhBytes)
  }

  protected def getInteger(params:Seq[String], idx:Int): Int = {
    params(idx).toInt
  }

  protected def getString(params:Seq[String], idx:Int): String = {
    params(idx)
  }

  protected def getAddress(params:Seq[String], idx:Int): Address = {
    cardanoOfflineAccess.translateBeth32ToAddress(params(idx))
  }

}

object ContractGenerator {


}