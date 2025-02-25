package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import proofspace.trustregistry.offchain.{ContractParameter, ContractParameterType}

enum ContractParameterTypeDTO {
  case TpInt
  case TpString(maxLen:Int, multiline: Boolean)
  case TpBool
}

object ContractParameterTypeDTO {
  given JsonValueCodec[ContractParameterTypeDTO] = JsonCodecMaker.make(
    CodecMakerConfig.withDiscriminatorFieldName(Some("tp"))
  )
}


case class ContractParameterTemplateDTO(
                                       name: String,
                                       description: String,
                                       tp: ContractParameterTypeDTO,
                                       optional: Boolean = false,
                                       )

object ContractParameterTemplateDTO {
  given JsonValueCodec[ContractParameterTemplateDTO] = JsonCodecMaker.make

  def fromContractParameter(contractParameter: ContractParameter): ContractParameterTemplateDTO = {
    contractParameter.tp match {
      case ContractParameterType.Integer =>
        ContractParameterTemplateDTO(contractParameter.name, contractParameter.description, ContractParameterTypeDTO.TpInt)
      case ContractParameterType.String =>
        ContractParameterTemplateDTO(contractParameter.name, contractParameter.description, ContractParameterTypeDTO.TpString(2048, true))
      case ContractParameterType.Address =>
        ContractParameterTemplateDTO(contractParameter.name, contractParameter.description, ContractParameterTypeDTO.TpString(64, false))
      case ContractParameterType.PubKeyHash =>
        ContractParameterTemplateDTO(contractParameter.name, contractParameter.description, ContractParameterTypeDTO.TpString(64, false))
      case ContractParameterType.Bool =>
        ContractParameterTemplateDTO(contractParameter.name, contractParameter.description, ContractParameterTypeDTO.TpBool)
      case ContractParameterType.Bytes =>
        ContractParameterTemplateDTO(contractParameter.name, contractParameter.description, ContractParameterTypeDTO.TpString(64532, true))
    }
  }

}

case class CardanoContractTemplateDTO(
                           name: String,
                           description: String,
                           parameters: Seq[ContractParameterTemplateDTO],
                             )

object CardanoContractTemplateDTO {
  given JsonValueCodec[CardanoContractTemplateDTO] = JsonCodecMaker.make
}

case class CardanoContractDTO(
                     registryName: String,
                     templateName: String,
                     parameters: Seq[String],
                             )

object CardanoContractDTO {
  given JsonValueCodec[CardanoContractDTO] = JsonCodecMaker.make
}

case class CardanoPlainContractDTO(
                     registryName: String,
                     targetAddress: String,
                     votingAddress: String,
                     targetMintingPolicy: String,
                     votinhMintingPolicy: String,
                             )
