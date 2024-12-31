package proofspace.trustregistry.fakes


import scalus.ledger.api.v3.{TxId, TxInfo, TxInInfo, TxOut, DatumHash, Datum, PubKeyHash, PosixTime}
import scalus.prelude.AssocMap

//TODO: pass time additonaly
class TxInfoAsOfflineAccess(txInfo: TxInfo, _time: PosixTime) extends proofspace.trustregistry.offchain.CardanoTransactionOfflineAccess {
  def id: TxId = txInfo.id
  def inputs: List[TxInInfo] = txInfo.inputs.toList
  def outputs: List[TxOut] = txInfo.outputs.toList
  def data: Map[DatumHash, Datum] = ledgerAssocMapToMap(txInfo.data)
  def signatories: List[PubKeyHash] = txInfo.signatories.toList
  def time: PosixTime = _time

  private def ledgerAssocMapToMap[K,V](am:  AssocMap[K,V]): Map[K,V] = {
    am.inner.toList.toMap
  }
}
