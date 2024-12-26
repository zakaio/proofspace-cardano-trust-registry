package proofspace.trustregistry.offchain

import scalus.ledger.api.v3.*

trait CardanoTransactionOfflineAccess {

  def id: TxId

  def inputs: List[TxInInfo]

  def outputs: List[TxOut]

  def data: Map[DatumHash, Datum]

  def signatories: List[PubKeyHash]

  def time: PosixTime

  //
  //  ideally we should implement this, but later.
  // def txInfo: TxInfo
}
