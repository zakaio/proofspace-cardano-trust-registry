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
   * Generate minting policy for trust registry. Minting policy usually cheks, that
   * @param pkh
   * @param name
   * @param contractParameters
   * @return
   */
  def generateMintingPolicy(name: String,
                            contractParameters: Seq[String],
                           ): scalus.uplc.Term

}
