package proofspace.trustregistry.gateways.cardano.contractTemplates

import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService
import proofspace.trustregistry.CardanoKeyConfig
import proofspace.trustregistry.dto.{CardanoContractDTO, CardanoContractTemplateDTO, ContractParameterTemplateDTO}
import proofspace.trustregistry.gateways.cardano.BFCardanoOffchainAccess
import proofspace.trustregistry.offchain.{CardanoOffchainAccess, SingleMaintainerGenerator}


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
  
}

object CardanoContractsService {
  
  case class ContractRecord(name: String,
                            template: CardanoContractTemplateDTO,
                            transactionBuilderFactory: (CardanoContractDTO, BFBackendService, String, Map[String, CardanoKeyConfig]) => ContractTransactionBuilder)

  def apply(contractTemplates: Map[String, ContractRecord]): CardanoContractsService = new CardanoContractsService(contractTemplates)

  def singkeMaintationTransactionBuilder(contract: CardanoContractDTO, bfService: BFBackendService, senderAddress: String, signerKeys: Map[String, CardanoKeyConfig]): ContractTransactionBuilder = {
    val cardanoOffchainAccess = BFCardanoOffchainAccess(bfService)
    val generator = new SingleMaintainerGenerator(cardanoOffchainAccess)
    new TemplateContractTransactionBuilder(contract, generator, bfService, senderAddress, signerKeys)
  }

  def singleRegistryMaintainerRecord: ContractRecord = {
     val parametersTemplates = SingleMaintainerGenerator.parametersDescription.map(
       ContractParameterTemplateDTO.fromContractParameter
     )
     val contractTemplate = CardanoContractTemplateDTO("singleRegistryMaintainer", "Single registry maintainer", parametersTemplates)
     ContractRecord("singleRegistryMaintainer", contractTemplate, singkeMaintationTransactionBuilder)
  }

  lazy val defaullTemplates: Map[String,ContractRecord] = Seq[ContractRecord](
    singleRegistryMaintainerRecord
  ).map(record => record.name -> record).toMap

  given CardanoContractsService = CardanoContractsService(defaullTemplates)

}

