package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

case class NetworkChoiceItemDTO(
                            network: String,
                            subnetworks: Seq[String]
                            )

case class NetworkChoiceDTO(
                      items: Seq[NetworkChoiceItemDTO]
                      )

object NetworkChoiceDTO {
  
    given JsonValueCodec[NetworkChoiceDTO] = JsonCodecMaker.make
  
}
