package proofspace.trustregistry.gateways.cardano.contractTemplates

import com.bloxbean.cardano.client.account.Account
import com.bloxbean.cardano.client.address.AddressProvider
import com.bloxbean.cardano.client.api.helper.TransactionBuilder
import com.bloxbean.cardano.client.api.model.{Amount, Utxo}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cps.*
import cps.monads.{*, given}
import proofspace.trustregistry.dto.*
import proofspace.trustregistry.gateways.cardano.{BlockfrostSessions, CardanoHelper}
import proofspace.trustregistry.offchain.*
import com.bloxbean.cardano.client.common.CardanoConstants
import com.bloxbean.cardano.client.transaction.*
import com.bloxbean.cardano.client.transaction.spec.{Value, Value as BloxbeanValue, *}
import com.bloxbean.cardano.client.backend.api.*
import com.bloxbean.cardano.client.backend.blockfrost.*
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService
import com.bloxbean.cardano.client.backend.model.TransactionContent
import com.bloxbean.cardano.client.common.model.{Network, Networks}
import com.bloxbean.cardano.client.function.helper.{InputBuilders, OutputBuilders, ScriptUtxoFinders, SignerProviders}
import com.bloxbean.cardano.client.function.{TxBuilder, TxBuilderContext, TxSigner}
import com.bloxbean.cardano.client.plutus.spec.{PlutusData, PlutusV3Script}
import com.bloxbean.cardano.client.quicktx.{QuickTxBuilder, ScriptTx, Tx}
import com.bloxbean.cardano.client.spec.Script
import proofspace.trustregistry.{AppConfig, CardanoKeyConfig}
import proofspace.trustregistry.model.{TrustRegistryDatum, TrustRegistryOperation}
import proofspace.trustregistry.model.TrustRegistryOperation.SetName
import scalus.*
import scalus.bloxbean.Interop
import scalus.builtin.ByteString
import scalus.builtin.Data.ToData

import java.math.BigInteger
import scala.jdk.CollectionConverters.*

class TemplateContractTransactionBuilder(contract: CardanoContractDTO,
                                         generator: ContractGenerator,
                                         bfService: BFBackendService,
                                         senderAddress: String,
                                         signerKeys: Map[String,CardanoKeyConfig]) extends ContractTransactionBuilder(bfService, senderAddress, signerKeys) {
  

  def buildCreateTransaction(name: String, snetwork: String): Future[String] = {
      val network = CardanoHelper.asNetwork(snetwork)
      val (targetAddress, targetScript) = targetAddressAndScript(name, network)
      
      
      val submitMintingPolicy = generator.generateSubmitMintingPolicy(name, contract.parameters).plutusV3
      val mintingPolicyScript = new MintingPolicyScript(submitMintingPolicy)

      val minAda = BigInt(CardanoConstants.ONE_ADA)
      val adaAmountToSend =
        if (generator.minChangeCost(contract.parameters) < minAda) minAda.bigInteger
        else generator.minChangeCost(contract.parameters).bigInteger

      val datum = TrustRegistryDatum.Operations(
        scalus.prelude.List.single(TrustRegistryOperation.SetName(ByteString.fromString(name)))
      )

      val retval = sendTransactionWithMintingPolicyAndAda(
        mintingPolicyScript.plutusScript,
        name,
        targetAddress,
        targetScript,
        adaAmountToSend,
        datum,
        createTxSigner(snetwork)
      )
      
      Future.successful(retval)
  }

  override def buildSubmitChangeTransaction(name: String, snetwork: String, change: TrustRegistryChangeDTO): Future[String] = {
      val network = CardanoHelper.asNetwork(snetwork)
      val (targetAddress, targetScript) = targetAddressAndScript(name, network)
      val submitMintingPolicy = generator.generateSubmitMintingPolicy(name, contract.parameters).plutusV3
      val operations = changeToOperations(change)
      if (scalus.prelude.List.isEmpty(operations)) {
        throw IllegalArgumentException("No operations to submit")
      }
      val datum = TrustRegistryDatum.Operations(operations)
      val mintingPolicyScript = new MintingPolicyScript(submitMintingPolicy)
      val retval = sendTransactionWithMintingPolicyAndAda(
        mintingPolicyScript.plutusScript,
        name,
        targetAddress,
        targetScript,
        generator.minChangeCost(contract.parameters).bigInteger,
        datum,
        createTxSigner(snetwork)
      )
      Future.successful(retval)
  }

  override def hasAppoveAndRejectTransactions: Boolean =
    generator.hasApprovalProcess


  override def buildApproveTransaction(name: String, snetwork: String, submitTransactionId: String): Future[String] = {
    if (!generator.hasApprovalProcess) {
      throw IllegalArgumentException("Approval process is not supported by the contract")
    }
    val network = CardanoHelper.asNetwork(snetwork)
    val (targetAddress, targetScript) = targetAddressAndScript(name, network)
    val approveMintingPolicy = generator.generateTargetMintingPolicy(name, contract.parameters).plutusV3
    val mintingPolicyScript = new MintingPolicyScript(approveMintingPolicy)
    val submitMintintPolicy = generator.generateSubmitMintingPolicy(name, contract.parameters).plutusV3
    val submitMintingPolicyScript = new MintingPolicyScript(submitMintintPolicy)
    val txSigner = createTxSigner(snetwork)
    val retrieveSubmitTransactionResult = bfService.getTransactionService.getTransaction(submitTransactionId)
    if (! retrieveSubmitTransactionResult.isSuccessful) then
      throw IllegalArgumentException(s"Transaction $submitTransactionId not found: ${retrieveSubmitTransactionResult.getResponse}")
    val submitTransaction: TransactionContent = retrieveSubmitTransactionResult.getValue

    val utxo = findOutputWithMintingPolicy(submitTransaction, submitMintingPolicyScript.plutusScript, name)

    val tx1 = ScriptTx().collectFrom(utxo)
      .payToContract(targetAddress, utxo.getAmount, PlutusData.unit(), targetScript)
      .mintAsset(mintingPolicyScript.plutusScript, List(Asset(name, BigInteger.valueOf(1))).asJava, PlutusData.unit(), targetAddress, PlutusData.unit())
    val tx2 = Tx().from(senderAddress)

    val tx = QuickTxBuilder(bfService).compose(tx1, tx2)
      .mergeOutputs(true)
      .withSigner(txSigner)
      .buildAndSign()

    val transactionId = sendTransaction(tx)
    Future.successful(transactionId)
  }

  override def buildRejectTransaction(name: String, snetwork: String,  submitTransactionId: String): Future[String] = {
    if (!generator.hasApprovalProcess) {
      throw IllegalArgumentException("Approval process is not supported by the contract")
    }
    val network = CardanoHelper.asNetwork(snetwork)
    val (targetAddress, targetScript) = targetAddressAndScript(name, network)
    val submitMintingPolicy = generator.generateSubmitMintingPolicy(name, contract.parameters).plutusV3
    val submitMintingPolicyScript = new MintingPolicyScript(submitMintingPolicy)
    val txSigner = createTxSigner(snetwork)
    val retrieveSubmitTransactionResult = bfService.getTransactionService.getTransaction(submitTransactionId)
    if (! retrieveSubmitTransactionResult.isSuccessful) then
      throw IllegalArgumentException(s"Transaction $submitTransactionId not found: ${retrieveSubmitTransactionResult.getResponse}")
    val submitTransaction: TransactionContent = retrieveSubmitTransactionResult.getValue

    val utxo = findOutputWithMintingPolicy(submitTransaction, submitMintingPolicyScript.plutusScript, name)

    val submitAssetUnit = submitMintingPolicyScript.scriptHash.toHex + name
    val utxoAsset = utxo.getAmount.asScala.find(_.getUnit == submitAssetUnit) match
      case Some(asset) => asset
      case None => throw IllegalArgumentException(s"Asset $submitAssetUnit not found in utxo from ${utxo.getTxHash}")

    val tx1 = ScriptTx().collectFrom(utxo)
      .mintAsset(submitMintingPolicyScript.plutusScript,
        List(Asset(submitAssetUnit, utxoAsset.getQuantity.negate())).asJava,
        PlutusData.unit(), targetAddress, PlutusData.unit())
    val tx = QuickTxBuilder(bfService).compose(tx1)
      .mergeOutputs(true)
      .withSigner(txSigner)
      .buildAndSign()

    val transactionId = sendTransaction(tx)
    Future successful transactionId
  }

  override def hasVoteTransaction: Boolean = false

  override def buildVoteTransaction(name: String, snetwork: String, submitTransactionId: String, approve: Boolean): Future[String] = ???
  
  def targetAddressAndScript(name: String, network: Network): (String, Script) = {
    val targetScalusScript = generator.generateTargetAddressScript(name, contract.parameters)
    val targetCborHex = targetScalusScript.plutusV3.doubleCborHex
    val script = PlutusV3Script.builder().cborHex(targetCborHex).build()
    val address = AddressProvider.getEntAddress(script, network)
    (address.toBech32, script)
  }

  override def buildTargetAddress(name: String, snetwork: String): Future[String] = {
    val tas = targetAddressAndScript(name, CardanoHelper.asNetwork(snetwork))
    Future.successful(tas._1)
  }


}

object TemplateContractTransactionBuilder {
  def apply(contract: CardanoContractDTO, generator: ContractGenerator, bfService: BFBackendService, senderAddress: String, signerKeys: Map[String,CardanoKeyConfig]): TemplateContractTransactionBuilder =
    new TemplateContractTransactionBuilder(contract, generator, bfService, senderAddress, signerKeys)
}
