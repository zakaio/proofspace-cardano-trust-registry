package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import sttp.tapir.EndpointIO.annotations.query


case class TrustRegistryEntryQueryDTO(
                                     @query
                                     registryId: String,
                                     @query
                                     limit: Option[Int] = None,
                                     @query
                                     offset: Option[Int] = None,
                                     @query
                                     orderBy: Option[String] = None,
                                     @query
                                     orderByDirection: Option[String] = None,
                                     @query
                                     did: Option[String] = None,
                                     ) 

object TrustRegistryEntryQueryDTO {
    
    given JsonValueCodec[TrustRegistryEntryQueryDTO] = JsonCodecMaker.make
    
}
