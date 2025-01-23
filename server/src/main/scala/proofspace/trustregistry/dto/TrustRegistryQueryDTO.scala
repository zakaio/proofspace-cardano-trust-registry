package proofspace.trustregistry.dto

import sttp.tapir.EndpointIO.annotations.query

case class TrustRegistryQueryDTO(
                                @query
                                registryId: Option[String] = None,
                                @query
                                name: Option[String] = None,
                                @query
                                limit: Option[Int] = None,
                                @query
                                offset: Option[Int] = None,
                                ) {
  
}
