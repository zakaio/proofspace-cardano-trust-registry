package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

case class CardanoGenericContractDTO(
                                      targetAddress: String,
                                      changeSubmitCost: Int,
                                      targetMintingPolicy: String,
                                      submitMintingPolicy: Option[String] = None,
                                      votingTokenPolicty: Option[String] = None,
                                      votingTokenAsset: Option[String] = None
                                    )

object CardanoGenericContractDTO {
  given JsonValueCodec[CardanoGenericContractDTO] = JsonCodecMaker.make
}



