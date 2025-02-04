package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import java.time.LocalDateTime

case class TrustRegistryDTO(
                             id: String,
                             name: String,
                             network: String,
                             proofspaceServiceDid: String,
                             proofspaceNetwork: String,
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
                                   subnetwork: Option[String] = None,
                                   proofspaceServiceDid: Option[String] = None,
                                   proofspaceNetwork: Option[String] = None,
                                   didPrefix: Option[String] = None,
                                   targetAdderss: Option[String] = None,
                                 )

object CreateTrustRegistryDTO {

  given JsonValueCodec[CreateTrustRegistryDTO] = JsonCodecMaker.make

}


case class TrustRegistriesDTO(
                              items: Seq[TrustRegistryDTO],
                              itemsTotal: Int
                            )

object TrustRegistriesDTO {

  given JsonValueCodec[TrustRegistriesDTO] = JsonCodecMaker.make

}