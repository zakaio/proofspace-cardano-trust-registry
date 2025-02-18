package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import sttp.tapir.EndpointIO.annotations.query


case class TrustRegistryChangeQueryDTO(
                                        @query
                                        registryId: Option[String] = None,
                                        @query
                                        changeId: Option[String] = None,
                                        @query
                                        limit: Option[Int] = None,
                                        @query
                                        offset: Option[Int] = None,
                                        // next options are filled from securityIn, so without @query annotation
                                        serviceDid: Option[String] = None,
                                        proofspaceNetwork: Option[String] = None,
                                        ) 

object TrustRegistryChangeQueryDTO {
    
    given JsonValueCodec[TrustRegistryChangeQueryDTO] = JsonCodecMaker.make
    
}
