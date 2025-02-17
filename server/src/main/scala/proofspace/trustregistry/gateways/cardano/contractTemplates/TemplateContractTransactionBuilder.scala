package proofspace.trustregistry.gateways.cardano.contractTemplates

import com.bloxbean.cardano.client.address.AddressProvider
import com.bloxbean.cardano.client.api.helper.TransactionBuilder

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cps.*
import cps.monads.{*, given}
import proofspace.trustregistry.dto.*
import proofspace.trustregistry.gateways.cardano.BlockfrostSessions
import proofspace.trustregistry.offchain.*
import com.bloxbean.cardano.client.common.CardanoConstants
import com.bloxbean.cardano.client.transaction.*
import com.bloxbean.cardano.client.transaction.spec.{Value as BloxbeanValue, *}
import com.bloxbean.cardano.client.backend.api.*
import com.bloxbean.cardano.client.backend.blockfrost.*
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService
import com.bloxbean.cardano.client.function.{TxBuilder, TxBuilderContext}
import com.bloxbean.cardano.client.plutus.spec.PlutusV3Script
import com.bloxbean.cardano.client.quicktx.{QuickTxBuilder, Tx}
import scalus.*
import scalus.bloxbean.Interop

class TemplateContractTransactionBuilder(generator: ContractGenerator, bfService: BFBackendService, senderAddress: String) extends ContractTransactionBuilder {


    def buildCreateTransaction(name: String, contract: CardanoContractDTO): Future[String] = async[Future]{
      val targetScalusScript = generator.generateTargetAddressScript(name, contract.parameters)
      val targetCborHex = targetScalusScript.plutusV3.doubleCborHex
      val targetPlutusScript = PlutusV3Script.builder().cborHex(targetCborHex).build()
      /*
      val targetScriptHash = targetPlutusScript.scriptHash

      val submitMintingPolicy = generator.generateSubmitMintingPolicy(name, contract.parameters).plutusV3
      val mintingPolicyScript = new MintingPolicyScript(submitMintingPolicy)
      val mintingPolicyId = mintingPolicyScript.scriptHash.toHex
      val minAda = BigInt(CardanoConstants.ONE_ADA)
      val amountToSend =
        if (generator.minChangeCost(contract.parameters) < minAda) minAda
        else generator.minChangeCost(contract.parameters)

     // val senderAddress =

      //val tx = (new Tx()).from(senderAddress)
      //  .payToAddress(targetAddress, )

      //val txBuilder = QuickTxBuilder(bfService)
      //txBuilder.compose()

      
      //val targetAddressBench32 = AddressProvider.getEntAddress()

      val transactionOutput = TransactionOutput.builder()
        .address(Interop.toAddress(targetAddress))
        .value(Value.builder().coin(BigInteger.ZERO).build())
        .build();


      val transactionService = bfService.getTransactionService
      val protocolParamsSupplier = new DefaultProtocolParamsSupplier(bfService.getEpochService)

      val value = BloxbeanValue.builder().multiAssets(
        List(
          MultiAsset.builder().policyId(mintingPolicyId).assetName(name).quantity(1).build(),

        ).asJava
      )
      */
      ???


    }

    override def buildSubmitChangeTransaction(name: String, contract: CardanoContractDTO): Future[String] =
      ???

    override def buildApproveTransaction(name: String, contract: CardanoContractDTO): Future[String] = ???

    override def buildRejectTransaction(name: String, contract: CardanoContractDTO): Future[String] = ???


}
