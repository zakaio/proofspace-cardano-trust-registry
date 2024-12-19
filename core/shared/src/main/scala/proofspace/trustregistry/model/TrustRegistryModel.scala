package proofspace.trustregistry.model

import scalus.builtin.*
import scalus.ledger.api.v3.TxId

trait TrustRegistrySnapshot {

  def id: TxId

  def time: Long

  def name: String

  def check(did: String): Boolean

  def applyChange(change: TrustRegistryChange, time: Long): TrustRegistrySnapshot

}


case class TrustRegistryChange(id: TxId, registryId: TxId, operations: List[TrustRegistryOperation])


enum TrustRegistryOperation {

  case AddDid(did: String)
  case RemoveDid(did: String)
  case ChangeTokenAddress(newTokenAddress: String)

}



