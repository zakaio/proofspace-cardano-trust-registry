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
import com.bloxbean.cardano.client.common.model.{Network, Networks}
import com.bloxbean.cardano.client.function.helper.{InputBuilders, OutputBuilders, ScriptUtxoFinders, SignerProviders}
import com.bloxbean.cardano.client.function.{TxBuilder, TxBuilderContext}
import com.bloxbean.cardano.client.plutus.spec.{PlutusData, PlutusV3Script}
import com.bloxbean.cardano.client.quicktx.{QuickTxBuilder, ScriptTx, Tx}
import com.bloxbean.cardano.client.spec.Script
import proofspace.trustregistry.{AppConfig, CardanoKeyConfig}
import proofspace.trustregistry.model.{TrustRegistryDatum, TrustRegistryOperation}
import proofspace.trustregistry.model.TrustRegistryOperation.SetName
import scalus.*
import scalus.bloxbean.Interop
import scalus.builtin.Data.ToData

import java.math.BigInteger
import scala.jdk.CollectionConverters.*

class TemplateContractTransactionBuilder(generator: ContractGenerator, bfService: BFBackendService, senderAddress: String, signerKeys: Map[String,CardanoKeyConfig]) extends ContractTransactionBuilder {

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


    def buildCreateTransaction(name: String, snetwork: String, contract: CardanoContractDTO): Future[String] = async[Future]{
      val network = asNetwork(snetwork)
      val (targetAddress, targetScript) = targetAddressAndScript(name, contract, network)
      
      
      val submitMintingPolicy = generator.generateSubmitMintingPolicy(name, contract.parameters).plutusV3
      val mintingPolicyScript = new MintingPolicyScript(submitMintingPolicy)
      val mintingPolicyId = mintingPolicyScript.scriptHash.toHex
      val minAda = BigInt(CardanoConstants.ONE_ADA)
      val adaAmountToSend =
        if (generator.minChangeCost(contract.parameters) < minAda) minAda.bigInteger
        else generator.minChangeCost(contract.parameters).bigInteger
      

      val datumScalusData = summon[ToData[TrustRegistryDatum]](TrustRegistryDatum.Operations(
        scalus.prelude.List.single(TrustRegistryOperation.SetName(scalus.builtin.ByteString.fromString(name)))
      ))

      
      val transactionService = bfService.getTransactionService
      
      val singerMnemonic = signerKeys.get(snetwork) match
        case Some(keyConfig) => keyConfig.seedPhrase.getOrElse(
          throw IllegalArgumentException(s"Seed phrase not found for network $snetwork")
        )
        case None => throw IllegalArgumentException(s"Signer key not found for network $snetwork")
      val signerAccount = new Account(network, singerMnemonic)
      val signer = SignerProviders.signerFrom(signerAccount)


      val tx1 = ScriptTx()
        .mintAsset(mintingPolicyScript.plutusScript,
          List(Asset(name,BigInteger.valueOf(1))).asJava,
          PlutusData.unit(),
          targetAddress,
          Interop.toPlutusData(datumScalusData)
        )
        .payToContract(targetAddress, Amount.lovelace(adaAmountToSend), PlutusData.unit(), targetScript)

      val tx2 = Tx().from(senderAddress)

      val tx = QuickTxBuilder(bfService).compose(tx1,tx2)
         .mergeOutputs(true)
        .withSigner(signer)
        .buildAndSign()

      val result = transactionService.submitTransaction(tx.serialize())
      if (!result.isSuccessful) {
        throw new IllegalStateException(s"Transaction submission failed: ${result.getResponse}")
      }
      val transactionId = result.getValue
      transactionId

    }

    override def buildSubmitChangeTransaction(name: String, snetwork: String, contract: CardanoContractDTO): Future[String] =
      ???

    override def buildApproveTransaction(name: String, snetwork: String, contract: CardanoContractDTO): Future[String] = ???

    override def buildRejectTransaction(name: String, snetwork: String, contract: CardanoContractDTO): Future[String] = ???


}
