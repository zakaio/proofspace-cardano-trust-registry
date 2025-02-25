package proofspace.trustregistry.gateways.cardano.contractTemplates

import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService
import proofspace.trustregistry.CardanoKeyConfig
import proofspace.trustregistry.dto.{CardanoContractDTO, CardanoContractTemplateDTO, ContractParameterTemplateDTO}
import proofspace.trustregistry.gateways.cardano.BFCardanoOffchainAccess
import proofspace.trustregistry.offchain.{CardanoOffchainAccess, ContractGenerator, SingleMaintainerGenerator, SubmitWithCostMaintainerApproveGenerator, UsingVotingTokensGenerator}


class CardanoContractsService(contractTemplates: Map[String, CardanoContractsService.ContractRecord]) {
  
   def listTemplates(): List[CardanoContractTemplateDTO] = {
     contractTemplates.values.map(_.template).toList
   }
  
   def  getTemplate(templateName: String): Option[CardanoContractTemplateDTO] = {
     contractTemplates.get(templateName).map(_.template)
   } 
  
   def getTransactionBuilder(contract: CardanoContractDTO ,
                             bFBackendService: BFBackendService,
                             senderAddress: String,
                             cardanoKeys: Map[String,CardanoKeyConfig]): Option[ContractTransactionBuilder] = {
     contractTemplates.get(contract.templateName).map(_.transactionBuilderFactory(contract, bFBackendService, senderAddress, cardanoKeys))
   }

   def contractPlutusGenerator(templateName: String): Option[CardanoOffchainAccess => ContractGenerator] = {
     contractTemplates.get(templateName).map(_.generator)
   }
  
}

object CardanoContractsService {
  
  case class ContractRecord(name: String,
                            template: CardanoContractTemplateDTO,
                            generator: CardanoOffchainAccess => ContractGenerator,
                            transactionBuilderFactory: (CardanoContractDTO, BFBackendService, String, Map[String, CardanoKeyConfig]) => ContractTransactionBuilder)

  def apply(contractTemplates: Map[String, ContractRecord]): CardanoContractsService = new CardanoContractsService(contractTemplates)

  def singleMaintationTransactionBuilder(contract: CardanoContractDTO, bfService: BFBackendService, senderAddress: String, signerKeys: Map[String, CardanoKeyConfig]): ContractTransactionBuilder = {
    val cardanoOffchainAccess = BFCardanoOffchainAccess(bfService)
    val generator = new SingleMaintainerGenerator(cardanoOffchainAccess)
    new TemplateContractTransactionBuilder(contract, generator, bfService, senderAddress, signerKeys)
  }

  def singleRegistryMaintainerRecord: ContractRecord = {
     val parametersTemplates = SingleMaintainerGenerator.parametersDescription.map(
       ContractParameterTemplateDTO.fromContractParameter
     )
     val contractTemplate = CardanoContractTemplateDTO("singleRegistryMaintainer", "Single registry maintainer", parametersTemplates)
     ContractRecord("singleRegistryMaintainer", contractTemplate, new SingleMaintainerGenerator(_),  singleMaintationTransactionBuilder)
  }



  def submitWithCostMaintainerApproveTransactionBuilder(contract: CardanoContractDTO, bfService: BFBackendService, senderAddress: String, signerKeys: Map[String, CardanoKeyConfig]): ContractTransactionBuilder = {
    val cardanoOffchainAccess = BFCardanoOffchainAccess(bfService)
    val generator = new SubmitWithCostMaintainerApproveGenerator(cardanoOffchainAccess)
    new TemplateContractTransactionBuilder(contract, generator, bfService, senderAddress, signerKeys)
  }

  def submitWithCostMaintainerApproveRecord: ContractRecord = {
    val parametersTemplates = SubmitWithCostMaintainerApproveGenerator.parametersDescription.map(
      ContractParameterTemplateDTO.fromContractParameter
    )
    val contractTemplate = CardanoContractTemplateDTO("submitWithCostMaintainerApprove", "Submit with cost maintainer approve", parametersTemplates)
    ContractRecord("submitWithCostMaintainerApprove", contractTemplate, new SubmitWithCostMaintainerApproveGenerator(_), submitWithCostMaintainerApproveTransactionBuilder )
  }

  def usingVotingTokensTransactionBuilder(contract: CardanoContractDTO, bfService: BFBackendService, senderAddress: String, signerKeys: Map[String, CardanoKeyConfig]): ContractTransactionBuilder = {
    val cardanoOffchainAccess = BFCardanoOffchainAccess(bfService)
    val generator = new UsingVotingTokensGenerator(cardanoOffchainAccess)
    new TemplateContractTransactionBuilder(contract, generator, bfService, senderAddress, signerKeys)
  }

  def usingVotingTokensRecord: ContractRecord = {
    val parametersTemplates = UsingVotingTokensGenerator.parametersDescription.map(
      ContractParameterTemplateDTO.fromContractParameter
    )
    val contractTemplate = CardanoContractTemplateDTO("usingVotingTokens", "Using voting tokens", parametersTemplates)
    ContractRecord("usingVotingTokens", contractTemplate, new UsingVotingTokensGenerator(_), usingVotingTokensTransactionBuilder)
  }


  lazy val defaullTemplates: Map[String,ContractRecord] = Seq[ContractRecord](
    singleRegistryMaintainerRecord,
    submitWithCostMaintainerApproveRecord,
    usingVotingTokensRecord
  ).map(record => record.name -> record).toMap

  given CardanoContractsService = CardanoContractsService(defaullTemplates)

}

