package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import java.time.LocalDateTime

case class TrustRegistryDTO(
                             id: String,
                             name: String,
                             network: String,
                             subnetwork: Option[String],
                             didPrefix: Option[String],
                             lastChangeDate: LocalDateTime,
                           )

object TrustRegistryDTO {
    
    given JsonValueCodec[TrustRegistryDTO] = JsonCodecMaker.make
}


case class CreateTrustRegistryDTO(
                                   name: String,
                                   network: String,
                                   subnetwork: String,
                                   didPrefix: Option[String],
                                   targetAdderss: Option[String],
                                 )

