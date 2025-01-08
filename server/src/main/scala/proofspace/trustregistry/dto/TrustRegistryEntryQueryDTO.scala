package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*


case class TrustRegistryEntryQueryDTO(
                                     limit: Option[Int],
                                     offset: Option[Int],
                                     orderBy: Option[String],
                                     orderByDirection: String,
                                     did: Option[String],
                                     status: Option[TrustRegistryEntryStatusDTO]
                                     ) 

object TrustRegistryEntryQueryDTO {
    
    given JsonValueCodec[TrustRegistryEntryQueryDTO] = JsonCodecMaker.make
    
}
