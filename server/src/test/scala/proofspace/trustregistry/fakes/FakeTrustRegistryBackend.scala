package proofspace.trustregistry.fakes

import proofspace.trustregistry.dto.TrustRegistryChangeDTO

import scala.collection.mutable.Queue
import scala.collection.mutable.Map as MutableMap
import proofspace.trustregistry.gateways.TrustRegistryBackend


class FakeTrustRegistryBackend extends TrustRegistryBackend {
  
  val byDid: MutableMap[String, FakeTrustRegistryBackend.Entry] = MutableMap.empty
  
  
}



object FakeTrustRegistryBackend {
  
  case class Entry(
    did: String,
    status: String,
    lastChange: TrustRegistryChangeDTO
                  )

  class InMemoryTrustRegistry {
    
  }
  
}
