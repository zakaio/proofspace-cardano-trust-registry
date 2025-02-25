package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

case class CardanoContractGenerateDTO(
                                     subnetwork: String,
                                     contract: CardanoContractDTO
                                     )

object CardanoContractGenerateDTO {
  given JsonValueCodec[CardanoContractGenerateDTO] = JsonCodecMaker.make
}
