package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*


case class TrustRegistryEntryQueryDTO(
                                     registryId: String,
                                     limit: Option[Int],
                                     offset: Option[Int],
                                     orderBy: Option[String],
                                     orderByDirection: Option[String],
                                     did: Option[String],
                                     status: Option[TrustRegistryEntryStatusDTO]
                                     ) 

object TrustRegistryEntryQueryDTO {
    
    given JsonValueCodec[TrustRegistryEntryQueryDTO] = JsonCodecMaker.make
    
}
