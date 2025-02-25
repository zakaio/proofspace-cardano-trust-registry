package proofspace.trustregistry.gateways.cardano.contractTemplates

import java.math.BigInteger
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService
import com.bloxbean.cardano.client.backend.model.TransactionContent
import com.bloxbean.cardano.client.common.CardanoConstants
import com.bloxbean.cardano.client.common.model.Network
import com.bloxbean.cardano.client.plutus.spec.{PlutusData, PlutusScript, PlutusV3Script}
import com.bloxbean.cardano.client.transaction.spec.Asset
import com.bloxbean.cardano.client.quicktx.{QuickTxBuilder, ScriptTx}
import com.bloxbean.cardano.client.spec.Script
import proofspace.trustregistry.CardanoKeyConfig
import proofspace.trustregistry.dto.{CardanoContractDTO, CardanoGenericContractDTO, TrustRegistryChangeDTO}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cps.*
import cps.monads.FutureAsyncMonad
import proofspace.trustregistry.controllers.HttpException
import proofspace.trustregistry.model.{TrustRegistryDatum, TrustRegistryOperation}
import proofspace.trustregistry.gateways.cardano.CardanoHelper
import scalus.builtin.ByteString
import scalus.utils.Hex
import scalus.bloxbean.Interop
import sttp.model.StatusCode

import scala.jdk.CollectionConverters.{*, given}


class GenericContractTransactionBuilder(contract: CardanoGenericContractDTO,
                                        bfService: BFBackendService,
                                        senderAddress: String,
                                        signerKeys: Map[String,CardanoKeyConfig],
                                        scriptRepository: ScriptRepository,
                               ) extends ContractTransactionBuilder(bfService, senderAddress, signerKeys) {

  override def buildCreateTransaction(name: String, snetwork: String): Future[String] = async[Future]{
    val network = CardanoHelper.asNetwork(snetwork)
    val targetOrSubmitMintingPolicy = contract.submitMintingPolicy.getOrElse(contract.targetMintingPolicy)
    val targetMintingScript = createMintingPolicyScript(targetOrSubmitMintingPolicy).await
    val targetAddressScript = createAddressScript(scriptHashFromBench32Address(contract.targetAddress)).await
    val targetAddress = buildTargetAddress(name, snetwork).await
    val minAda = BigInt(CardanoConstants.ONE_ADA)
    val adaAmountToSend = BigInteger.valueOf(contract.changeSubmitCost)
    val datum = TrustRegistryDatum.Operations(
      scalus.prelude.List.single(TrustRegistryOperation.SetName(ByteString.fromString(name)))
    )
    val txSigner = createTxSigner(snetwork)
    val tx = sendTransactionWithMintingPolicyAndAda(
      targetMintingScript,
      name,
      targetAddress,
      targetAddressScript,
      adaAmountToSend,
      datum,
      txSigner,
    )

    tx
  }
  
  override def buildSubmitChangeTransaction(name: String, snetwork: String, change: TrustRegistryChangeDTO): Future[String] =  async[Future] {
    val network = CardanoHelper.asNetwork(snetwork)
    val targetAddressScript = createAddressScript(scriptHashFromBench32Address(contract.targetAddress)).await
    val targetAddress = buildTargetAddress(name, snetwork).await
    val targetOrSubmitMintingPolicy = contract.submitMintingPolicy.getOrElse(contract.targetMintingPolicy)
    val mintingPolicy = createMintingPolicyScript(targetOrSubmitMintingPolicy).await
    val operations = changeToOperations(change)
    if (scalus.prelude.List.isEmpty(operations)) {
      throw IllegalArgumentException("No operations to submit")
    }
    val datum = TrustRegistryDatum.Operations(operations)
    val retval = sendTransactionWithMintingPolicyAndAda(
      mintingPolicy,
      name,
      targetAddress,
      targetAddressScript,
      BigInteger.valueOf(contract.changeSubmitCost),
      datum,
      createTxSigner(snetwork)
    )
    retval

  }

  override def hasAppoveAndRejectTransactions: Boolean = {
    contract.submitMintingPolicy.isDefined
  }
  
  override def buildApproveTransaction(name: String, snetwork: String, submitTransactionId: String): Future[String] = async[Future] {
    val network = CardanoHelper.asNetwork(snetwork)
    val targetAddressScript = createAddressScript(scriptHashFromBench32Address(contract.targetAddress)).await
    val targetAddress = buildTargetAddress(name, snetwork).await
    val targetMintingPolicy = createMintingPolicyScript(contract.targetMintingPolicy).await

    val submitMintingPolicyScript = createMintingPolicyScript(contract.submitMintingPolicy.get).await
    val txSigner = createTxSigner(snetwork)
    val retrieveSubmitTransactionResult = bfService.getTransactionService.getTransaction(submitTransactionId)
    if (!retrieveSubmitTransactionResult.isSuccessful) then
      throw IllegalArgumentException(s"Transaction $submitTransactionId not found: ${retrieveSubmitTransactionResult.getResponse}")
    val submitTransaction: TransactionContent = retrieveSubmitTransactionResult.getValue

    val utxo = findOutputWithMintingPolicy(submitTransaction, submitMintingPolicyScript, name)

    val submitAssetUnit = scalus.utils.Hex.bytesToHex(submitMintingPolicyScript.getScriptHash) + name
    val utxoAsset = utxo.getAmount.asScala.find(_.getUnit == submitAssetUnit) match
      case Some(asset) => asset
      case None => throw IllegalArgumentException(s"Asset $submitAssetUnit not found in utxo from ${utxo.getTxHash}")

    val tx1 = ScriptTx().collectFrom(utxo)
      .mintAsset(submitMintingPolicyScript,
        List(Asset(submitAssetUnit, utxoAsset.getQuantity.negate())).asJava,
        PlutusData.unit(), targetAddress, PlutusData.unit())
    val tx = QuickTxBuilder(bfService).compose(tx1)
      .mergeOutputs(true)
      .withSigner(txSigner)
      .buildAndSign()

    val transactionId = sendTransaction(tx)
    transactionId
  }

  override def buildRejectTransaction(name: String, snetwork: String, submitTransactionId: String): Future[String] = async[Future]{
    if (!hasAppoveAndRejectTransactions) {
      throw IllegalArgumentException("Approval process is not supported by the contract")
    }
    val network = CardanoHelper.asNetwork(snetwork)
    val targetAddressScript = createAddressScript(scriptHashFromBench32Address(contract.targetAddress)).await
    val targetAddress = buildTargetAddress(name, snetwork).await
    val submitMintingPolicyScript = createMintingPolicyScript(contract.submitMintingPolicy.get).await
    val txSigner = createTxSigner(snetwork)
    val retrieveSubmitTransactionResult = bfService.getTransactionService.getTransaction(submitTransactionId)
    if (!retrieveSubmitTransactionResult.isSuccessful) then
      throw IllegalArgumentException(s"Transaction $submitTransactionId not found: ${retrieveSubmitTransactionResult.getResponse}")
    val submitTransaction: TransactionContent = retrieveSubmitTransactionResult.getValue

    val utxo = findOutputWithMintingPolicy(submitTransaction, submitMintingPolicyScript, name)

    val submitAssetUnit = scalus.utils.Hex.bytesToHex(submitMintingPolicyScript.getScriptHash) + name
    val utxoAsset = utxo.getAmount.asScala.find(_.getUnit == submitAssetUnit) match
      case Some(asset) => asset
      case None => throw IllegalArgumentException(s"Asset $submitAssetUnit not found in utxo from ${utxo.getTxHash}")

    val tx1 = ScriptTx().collectFrom(utxo)
      .mintAsset(submitMintingPolicyScript,
        List(Asset(submitAssetUnit, utxoAsset.getQuantity.negate())).asJava,
        PlutusData.unit(), targetAddress, PlutusData.unit())
    val tx = QuickTxBuilder(bfService).compose(tx1)
      .mergeOutputs(true)
      .withSigner(txSigner)
      .buildAndSign()

    val transactionId = sendTransaction(tx)
    transactionId
  }


  def createMintingPolicyScript(hash: String): Future[PlutusScript] = async[Future] {
    val script = scriptRepository.getScriptByHash(hash).await.getOrElse(
      throw HttpException(StatusCode.UnprocessableEntity, s"Script not found for hash $hash")
    )
    val mintingPolicyScript = PlutusV3Script.builder().cborHex(Hex.bytesToHex(script)).build()
    mintingPolicyScript
  }

  def createAddressScript(hash: String): Future[Script] = async[Future] {
    val script = scriptRepository.getScriptByHash(hash).await.getOrElse(
      throw HttpException(StatusCode.UnprocessableEntity, s"Script not found for hash $hash")
    )
    val addressScript = PlutusV3Script.builder().cborHex(Hex.bytesToHex(script)).build()
    addressScript
  }

  def scriptHashFromBench32Address(bench32addr: String): String = {
    val addr = new com.bloxbean.cardano.client.address.Address(bench32addr)
    if (addr.getPaymentCredentialHash.isEmpty) {
      throw IllegalArgumentException(s"Address $bench32addr is not a script address")
    }
    val scriptHash = addr.getPaymentCredentialHash.get
    Hex.bytesToHex(scriptHash)
  }

  override def buildTargetAddress(name: String, snetwork: String): Future[String] = {
    Future successful contract.targetAddress
  }

  override def hasVoteTransaction: Boolean = false

  override def buildVoteTransaction(name: String, snetwork: String, submitTransactionId: String, approve: Boolean): Future[String] = ???


}
