package proofspace.trustregistry.gateways.cardano.contractTemplates

import scala.concurrent.*
import proofspace.trustregistry.dto.CardanoContractDTO

trait ContractTransactionBuilder {

  def buildCreateTransaction(name: String, snetwork: String, contract: CardanoContractDTO): Future[String]

  def buildSubmitChangeTransaction(name: String, snetwork: String, contract: CardanoContractDTO): Future[String]

  def buildApproveTransaction(name: String, snetwork: String, contract: CardanoContractDTO): Future[String]

  def buildRejectTransaction(name: String, snetwork: String, contract: CardanoContractDTO): Future[String]


}
