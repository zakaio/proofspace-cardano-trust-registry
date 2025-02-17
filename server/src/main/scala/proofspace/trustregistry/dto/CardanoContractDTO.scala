package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

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
