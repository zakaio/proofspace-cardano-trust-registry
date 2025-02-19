package proofspace.trustregistry.gateways.cardano.contractTemplates

import com.bloxbean.cardano.client.account.Account
import com.bloxbean.cardano.client.address.AddressProvider
import com.bloxbean.cardano.client.api.helper.TransactionBuilder
import com.bloxbean.cardano.client.api.model.Amount

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cps.*
import cps.monads.{*, given}
import proofspace.trustregistry.dto.*
import proofspace.trustregistry.gateways.cardano.BlockfrostSessions
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

class TemplateContractTransactionBuilder(generator: ContractGenerator, bfService: BFBackendService, senderAddress: String, signerKeys: Map[String,CardanoKeyConfig]) extends ContractTransactionBuilder {




  def buildCreateTransaction(name: String, snetwork: String, contract: CardanoContractDTO): Future[String] = {
      val network = asNetwork(snetwork)
      val (targetAddress, targetScript) = targetAddressAndScript(name, contract, network)
      
      
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
        mintingPolicyScript,
        name,
        targetAddress,
        targetScript,
        adaAmountToSend,
        datum,
        createTxSigner(snetwork)
      )
      
      Future.successful(retval)
  }

  override def buildSubmitChangeTransaction(name: String, snetwork: String, contract: CardanoContractDTO, change: TrustRegistryChangeDTO): Future[String] = {
      val network = asNetwork(snetwork)
      val (targetAddress, targetScript) = targetAddressAndScript(name, contract, network)
      val submitMintingPolicy = generator.generateSubmitMintingPolicy(name, contract.parameters).plutusV3
      val operations = changeToOperations(change)
      if (scalus.prelude.List.isEmpty(operations)) {
        throw IllegalArgumentException("No operations to submit")
      }
      val datum = TrustRegistryDatum.Operations(operations)
      val mintingPolicyScript = new MintingPolicyScript(submitMintingPolicy)
      val retval = sendTransactionWithMintingPolicyAndAda(
        mintingPolicyScript,
        name,
        targetAddress,
        targetScript,
        generator.minChangeCost(contract.parameters).bigInteger,
        datum,
        createTxSigner(snetwork)
      )
      Future.successful(retval)
  }

  override def hasAppoveAndRejectTransactions(contract: CardanoContractDTO): Boolean =
    generator.hasApprovalProcess


  override def buildApproveTransaction(name: String, snetwork: String, contract: CardanoContractDTO, submitTransactionId: String): Future[String] = {
    val network = asNetwork(snetwork)
    val (targetAddress, targetScript) = targetAddressAndScript(name, contract, network)
    val approveMintingPolicy = generator.generateTargetMintingPolicy(name, contract.parameters).plutusV3
    val mintingPolicyScript = new MintingPolicyScript(approveMintingPolicy)
    val submitMintintPolicy = generator.generateSubmitMintingPolicy(name, contract.parameters).plutusV3
    val submitMintingPolicyScript = new MintingPolicyScript(submitMintintPolicy)
    val txSigner = createTxSigner(snetwork)
    val retrieveSubmitTransactionResult = bfService.getTransactionService.getTransaction(submitTransactionId)
    if (! retrieveSubmitTransactionResult.isSuccessful) then
      throw IllegalArgumentException(s"Transaction $submitTransactionId not found: ${retrieveSubmitTransactionResult.getResponse}")
    val submitTransaction: TransactionContent = retrieveSubmitTransactionResult.getValue

    // TODO: check,  is it correct?  Maybe we need name to be in hex ?
    val submitAssetUnit = submitMintingPolicyScript.scriptHash.toHex + name

    val outputAmounts = submitTransaction.getOutputAmount.asScala
    val existsOutAmount = outputAmounts.exists(_.getUnit == submitAssetUnit)
    if (!existsOutAmount) then
      throw IllegalArgumentException(s"Output with asset $submitAssetUnit not found in transaction $submitTransactionId")

    val utxosCount = submitTransaction.getUtxoCount
    // now search for utxos
    val utxos = (0 until utxosCount).flatMap { utxoIndex =>
      val r = bfService.getUtxoService.getTxOutput(submitTransactionId, utxoIndex)
      if (!r.isSuccessful) then
        throw IllegalArgumentException(s"Error fetching utxo $utxoIndex for transaction $submitTransactionId: ${r.getResponse}")
      val utxo = r.getValue
      val amounts = utxo.getAmount.asScala
      amounts.find(_.getUnit == submitAssetUnit ).map(_ => utxo)
    }

    if (utxos.isEmpty) then
      throw IllegalArgumentException(s"Utxo with asset $submitAssetUnit not found in transaction $submitTransactionId")

    if (utxos.size > 1) then
      throw IllegalArgumentException(s"Multiple utxos with asset $submitAssetUnit found in transaction $submitTransactionId")

    val utxo = utxos.head
    val tx1 = ScriptTx().collectFrom(utxo)
      .payToContract(targetAddress, utxo.getAmount, PlutusData.unit(), targetScript)
      .mintAsset(mintingPolicyScript.plutusScript, List(Asset(name, BigInteger.valueOf(1))).asJava, PlutusData.unit(), targetAddress, PlutusData.unit())
    val tx2 = Tx().from(senderAddress)

    val tx = QuickTxBuilder(bfService).compose(tx1, tx2)
      .mergeOutputs(true)
      .withSigner(txSigner)
      .buildAndSign()

    ???


  }


  override def buildRejectTransaction(name: String, snetwork: String, contract: CardanoContractDTO,  submitTransactionId: String): Future[String] = ???

  override def hasVoteTransaction(contract: CardanoContractDTO): Boolean = false

  override def buildVoteTransaction(name: String, snetwork: String, contract: CardanoContractDTO, submitTransactionId: String, approve: Boolean): Future[String] = ???

  def asNetwork(snetwork: String): Network = {
    snetwork match
      case "mainnet" => Networks.mainnet()
      case "testnet" => Networks.testnet()
      case "preprod" => Networks.preprod()
      case "preview" => Networks.preview()
      case _ => throw IllegalArgumentException(s"Invalid network $snetwork")
  }

  def targetAddressAndScript(name: String, contract: CardanoContractDTO, network: Network): (String, Script) = {
    val targetScalusScript = generator.generateTargetAddressScript(name, contract.parameters)
    val targetCborHex = targetScalusScript.plutusV3.doubleCborHex
    val script = PlutusV3Script.builder().cborHex(targetCborHex).build()
    val address = AddressProvider.getEntAddress(script, network)
    (address.toBech32, script)
  }

  def changeToOperations(change: TrustRegistryChangeDTO): scalus.prelude.List[TrustRegistryOperation] = {
    val addedDids = scalus.prelude.List.from(change.addedDids.map(did => ByteString.fromString(did)))
    val removedDids = scalus.prelude.List.from(change.removedDids.map(did => ByteString.fromString(did)))
    if (scalus.prelude.List.isEmpty(addedDids) && scalus.prelude.List.isEmpty(removedDids)) {
      scalus.prelude.List.empty
    } else if (scalus.prelude.List.isEmpty(addedDids)) {
      scalus.prelude.List.single(TrustRegistryOperation.RemoveDids(removedDids))
    } else if (scalus.prelude.List.isEmpty(removedDids)) {
      scalus.prelude.List.single(TrustRegistryOperation.AddDids(addedDids))
    } else {
      scalus.prelude.List.from(Seq(
        TrustRegistryOperation.AddDids(addedDids),
        TrustRegistryOperation.RemoveDids(removedDids)
      ))
    }
  }

  def createTxSigner(snetwork: String): TxSigner = {
    val singerMnemonic = signerKeys.get(snetwork) match
      case Some(keyConfig) => keyConfig.seedPhrase.getOrElse(
        throw IllegalArgumentException(s"Seed phrase not found for network $snetwork")
      )
      case None => throw IllegalArgumentException(s"Signer key not found for network $snetwork")
    val network = asNetwork(snetwork)
    val signerAccount = new Account(network, singerMnemonic)
    val signer = SignerProviders.signerFrom(signerAccount)
    signer
  }

  def sendTransactionWithMintingPolicyAndAda(mintingPolicyScript: MintingPolicyScript,
                                             assetName: String,
                                             targetAddress: String,
                                             targetScript: Script,
                                             adaAmountToSend: BigInteger,
                                             datum: TrustRegistryDatum,
                                             signer: TxSigner): String = {
    val datumScalusData = summon[ToData[TrustRegistryDatum]](datum)
    val tx1 = ScriptTx()
      .mintAsset(mintingPolicyScript.plutusScript,
        List(Asset(assetName,BigInteger.valueOf(1))).asJava,
        PlutusData.unit(),
        targetAddress,
        Interop.toPlutusData(datumScalusData)
      )
      .payToContract(targetAddress, Amount.lovelace(adaAmountToSend), PlutusData.unit(), targetScript)
    val tx2 = Tx().from(senderAddress)
    val tx = QuickTxBuilder(bfService).compose(tx1, tx2)
      .mergeOutputs(true)
      .withSigner(signer)
      .buildAndSign()

    val transactionService = bfService.getTransactionService
    val result = transactionService.submitTransaction(tx.serialize())
    if (!result.isSuccessful) {
      throw new IllegalStateException(s"Transaction submission failed: ${result.getResponse}")
    }
    val transactionId = result.getValue
    transactionId
  }

}
