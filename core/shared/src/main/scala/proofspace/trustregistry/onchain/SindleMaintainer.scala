package proofspace.trustregistry.onchain

import scalus.ledger.api.v3.{PubKeyHash, ScriptContext}

@scalus.Compile
object SindleMaintainer  {
  
  def listContains(pkh: PubKeyHash, list: scalus.prelude.List[PubKeyHash]): Boolean = {
    list match
      case scalus.prelude.List.Nil => false
      case scalus.prelude.List.Cons(head, tail) =>
        if head == pkh then true
        else listContains(pkh, tail)
  }
  
  /**
   * Check, if the given public key hash is verified by the maintainer.
   * We assume that changes in the trust registry are packet in the transaction,
   * but do not verify this.
   */
  def verifiedByMin(pkh: PubKeyHash)(ctx: ScriptContext) : Boolean = {
      listContains(pkh, ctx.txInfo.signatories)
  }
  
  
}
