package proofspace.trustregistry.gateways.cardano.contractTemplates

import scala.concurrent.*
import proofspace.trustregistry.dto.{CardanoContractDTO, TrustRegistryChangeDTO}

trait ContractTransactionBuilder {

  def buildCreateTransaction(name: String, snetwork: String, contract: CardanoContractDTO): Future[String]
  
  def buildSubmitChangeTransaction(name: String, snetwork: String, contract: CardanoContractDTO, change: TrustRegistryChangeDTO): Future[String]

  def hasAppoveAndRejectTransactions(contract: CardanoContractDTO): Boolean 
  
  def buildApproveTransaction(name: String, snetwork: String, contract: CardanoContractDTO, submitTransactionId: String): Future[String]

  def buildRejectTransaction(name: String, snetwork: String, contract: CardanoContractDTO, submitTransactionId: String): Future[String]

  def hasVoteTransaction(contract: CardanoContractDTO): Boolean
  
  def buildVoteTransaction(name: String, snetwork: String, contract: CardanoContractDTO, submitTransactionId: String, approve: Boolean): Future[String]

}
