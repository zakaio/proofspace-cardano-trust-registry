package proofspace.trustregistry.gateways.cardano.contractTemplates

import proofspace.trustregistry.dto.CardanoContractTemplateDTO


class ContractContractsService(contractTemplates: Map[String, ContractContractsService.ContractRecord]) {
  
   def listTemplates(): List[CardanoContractTemplateDTO] = {
     contractTemplates.values.map(_.template).toList
   }
  
   def  getTemplate(templateName: String): Option[CardanoContractTemplateDTO] = {
     contractTemplates.get(templateName).map(_.template)
   } 
  
   def getTransactionBuilder(templateName: String): Option[ContractTransactionBuilder] = {
     contractTemplates.get(templateName).map(_.transactionBuilder)
   }
  
}

object ContractContractsService {
  
  case class ContractRecord(name: String, template: CardanoContractTemplateDTO, transactionBuilder: ContractTransactionBuilder)
  
  def apply(contractTemplates: Map[String, ContractRecord]): ContractContractsService = new ContractContractsService(contractTemplates)
  
}