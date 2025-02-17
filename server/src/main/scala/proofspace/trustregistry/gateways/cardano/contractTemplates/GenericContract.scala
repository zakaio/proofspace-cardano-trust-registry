package proofspace.trustregistry.gateways.cardano.contractTemplates

import proofspace.trustregistry.dto.*
import proofspace.trustregistry.gateways.cardano.BlockfrostSessions


object GenericContract {

  val template: CardanoContractTemplateDTO = CardanoContractTemplateDTO(
    name="generic",
    description = "Generic contract template",
    parameters = Seq(
      ContractParameterTemplateDTO(
        name = "target address",
        description = "Address where submit the approved transaction",
        tp = ContractParameterTypeDTO.TpString(40, multiline = false)
      ),
      ContractParameterTemplateDTO(
        name = "target minting policy",
        description = "Minting policy for the target address",
        tp = ContractParameterTypeDTO.TpString(40, multiline = false),
        optional = false
      ),
      ContractParameterTemplateDTO(
        name = "submit minting policy",
        description = "Minting policy for the target address",
        tp = ContractParameterTypeDTO.TpString(40, multiline = false)
      ),
      ContractParameterTemplateDTO(
        name = "submit cost",
        description = "Cost of submitting transaction (in lovelace)",
        tp = ContractParameterTypeDTO.TpInt
      ),
      ContractParameterTemplateDTO(
        name = "submit cost",
        description = "Cost of submitting transaction (in lovelace)",
        tp = ContractParameterTypeDTO.TpString(40, multiline = false)
      ),
      ContractParameterTemplateDTO(
        name = "voting token",
        description = "Voting token",
        tp = ContractParameterTypeDTO.TpString(40, multiline = false),
        optional = true,
      ),
      ContractParameterTemplateDTO(
        name = "voting token asset",
        description = "Voting token asset",
        tp = ContractParameterTypeDTO.TpString(40, multiline = false),
        optional = true,
      ),
      ContractParameterTemplateDTO(
        name = "change cost voting token",
        description = "Cost of change in voting tokens",
        tp = ContractParameterTypeDTO.TpInt,
        optional = true,
      )
    ),
  )

}

/*
class GenericTransactionBuilder(session: BlockfrostSessions) extends ContractTransactionBuilder {

  


}
*/