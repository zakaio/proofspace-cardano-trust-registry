package proofspace.trustregistry.gateways.cardano.contractTemplates

import com.bloxbean.cardano.client.account.Account
import com.bloxbean.cardano.client.api.model.{Amount, Utxo}
import com.bloxbean.cardano.client.backend.api.*
import com.bloxbean.cardano.client.backend.blockfrost.*
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService
import com.bloxbean.cardano.client.backend.model.TransactionContent
import com.bloxbean.cardano.client.common.model.{Network, Networks}
import com.bloxbean.cardano.client.function.TxSigner
import com.bloxbean.cardano.client.function.helper.SignerProviders
import com.bloxbean.cardano.client.plutus.spec.{PlutusData, PlutusScript}
import com.bloxbean.cardano.client.quicktx.{QuickTxBuilder, ScriptTx, Tx}
import com.bloxbean.cardano.client.spec.Script
import com.bloxbean.cardano.client.plutus.spec.PlutusScript
import com.bloxbean.cardano.client.transaction.spec.{Asset, Transaction}

import scala.concurrent.*
import scala.jdk.CollectionConverters.*
import proofspace.trustregistry.model.*
import proofspace.trustregistry.dto.{CardanoContractDTO, TrustRegistryChangeDTO}
import proofspace.trustregistry.*
import proofspace.trustregistry.gateways.cardano.CardanoHelper
import scalus.bloxbean.Interop
import scalus.prelude.{List as _, *}
import scalus.prelude.Prelude.*
import scalus.builtin.Data.ToData
import scalus.builtin.ByteString

import java.math.BigInteger

abstract class ContractTransactionBuilder(bfService: BFBackendService,senderAddress: String,
                                          signerKeys: Map[String,CardanoKeyConfig]) {

  def buildTargetAddress(name: String, snetwork: String): Future[String]

  def buildCreateTransaction(name: String, snetwork: String): Future[String]
  
  def buildSubmitChangeTransaction(name: String, snetwork: String, change: TrustRegistryChangeDTO): Future[String]

  def hasAppoveAndRejectTransactions: Boolean
  
  def buildApproveTransaction(name: String, snetwork: String, submitTransactionId: String): Future[String]

  def buildRejectTransaction(name: String, snetwork: String, submitTransactionId: String): Future[String]

  def hasVoteTransaction: Boolean
  
  def buildVoteTransaction(name: String, snetwork: String, submitTransactionId: String, approve: Boolean): Future[String]


  protected def sendTransaction(tx: Transaction): String = {
    val transactionService = bfService.getTransactionService
    val result = transactionService.submitTransaction(tx.serialize())
    if (!result.isSuccessful) {
      throw new IllegalStateException(s"Transaction submission failed: ${result.getResponse}")
    }
    val transactionId = result.getValue
    transactionId
  }

  protected def sendTransactionWithMintingPolicyAndAda(mintingPolicyScript: PlutusScript,
                                              assetName: String,
                                              targetAddress: String,
                                              targetScript: Script,
                                              adaAmountToSend: BigInteger,
                                              datum: TrustRegistryDatum,
                                              signer: TxSigner): String = {
    val datumScalusData = summon[ToData[TrustRegistryDatum]](datum)
    val tx1 = ScriptTx()
      .mintAsset(mintingPolicyScript,
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

    val transactionId = sendTransaction(tx)
    transactionId
  }


  protected def createTxSigner(snetwork: String): TxSigner = {
    val singerMnemonic = signerKeys.get(snetwork) match
      case Some(keyConfig) => keyConfig.seedPhrase.getOrElse(
        throw IllegalArgumentException(s"Seed phrase not found for network $snetwork")
      )
      case None => throw IllegalArgumentException(s"Signer key not found for network $snetwork")
    val network = CardanoHelper.asNetwork(snetwork)
    val signerAccount = new Account(network, singerMnemonic)
    val signer = SignerProviders.signerFrom(signerAccount)
    signer
  }

  protected def changeToOperations(change: TrustRegistryChangeDTO): scalus.prelude.List[TrustRegistryOperation] = {
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

  protected def findOutputWithMintingPolicy(submitTransaction: TransactionContent, mintingPolicyScript: PlutusScript, name: String): Utxo = {
    //val submitAssetUnit = mintingPolicyScript.scriptHash.toHex + name
    val submitAssetUnit = scalus.utils.Hex.bytesToHex(mintingPolicyScript.getScriptHash)+name

    val outputAmounts = submitTransaction.getOutputAmount.asScala
    val existsOutAmount = outputAmounts.exists(_.getUnit == submitAssetUnit)
    if (!existsOutAmount) then
      throw IllegalArgumentException(s"Output with asset $submitAssetUnit not found in transaction ${submitTransaction.getHash}")


    val utxosCount = submitTransaction.getUtxoCount
    // now search for utxos
    val utxos = (0 until utxosCount).flatMap { utxoIndex =>
      val r = bfService.getUtxoService.getTxOutput(submitTransaction.getHash, utxoIndex)
      if (!r.isSuccessful) then
        throw IllegalArgumentException(s"Error fetching utxo $utxoIndex for transaction ${submitTransaction.getHash}: ${r.getResponse}")
      val utxo = r.getValue
      val amounts = utxo.getAmount.asScala
      amounts.find(_.getUnit == submitAssetUnit).map(_ => utxo)
    }

    if (utxos.isEmpty) then
      throw IllegalArgumentException(s"Utxo with asset $submitAssetUnit not found in transaction ${submitTransaction.getHash}")

    if (utxos.size > 1) then
      throw IllegalArgumentException(s"Multiple utxos with asset $submitAssetUnit found in transaction ${submitTransaction.getHash}")

    utxos.head

  }


}
