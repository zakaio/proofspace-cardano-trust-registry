package proofspace.trustregistry.onchain

import proofspace.trustregistry.common.PreludeListData
import proofspace.trustregistry.model.{TrustRegistryChange, TrustRegistryOperation}
import scalus.builtin.{ByteString, Data}
import scalus.builtin.Data.FromData
import scalus.ledger.api.v2.OutputDatum
import scalus.ledger.api.v3.*
import scalus.prelude.Maybe.Just
import scalus.prelude.{AssocMap, Maybe}
import scalus.prelude.Prelude.{*, given}

/**
 * Single maintainer trust registry.
 * The check in minting policy is done that:
 * 1. The output is signed by the maintainer
 * 2. The output contains the trust registry operations
 * 3. The output is named by the given name
 */
@scalus.Compile
object SindleMaintainer  {


  /**
   * Check, if the given public key hash is verified by the maintainer.
   * We assume that changes in the trust registry are packet in the transaction,
   * but do not verify this.
   */
  def verifyPkh(pkh: PubKeyHash)(ctx: ScriptContext) = {
       scalus.prelude.List.findOrFail(ctx.txInfo.signatories)(_ == pkh)
  }




  /**
   * Check, if the given data is a valid trust registry operations.
   */
  def verifyDatum(datum: Data): Boolean = {
    val operations = PreludeListData.listFromData[TrustRegistryOperation](datum)
    !(scalus.prelude.List.isEmpty(operations))
  }

  /**
   * Check minting policy for the single-maintainer trust registry.
   */
  def mintingPolicy(pkh: PubKeyHash, registryName: ByteString)(ctx: ScriptContext): Unit = {
    val txInfo = ctx.txInfo

    val ownSym = ctx.scriptInfo match
      case ScriptInfo.MintingScript(sym) => sym
      case _ => throw new Exception("Minting script is expected")

    val unused = verifyPkh(pkh)(ctx)

    val myOutputs = scalus.prelude.List.filter(txInfo.outputs){
      txOut =>
        AssocMap.lookup(txOut.value)(ownSym) match
          case Just(byNames) =>
            AssocMap.lookup(byNames)(registryName) match
              case Just(v) =>
                val datum = txOut.datum match
                  case OutputDatum.NoOutputDatum => throw new Exception("No datum in the output")
                  case OutputDatum.OutputDatum(d) => d
                  case OutputDatum.OutputDatumHash(datumHash) =>
                    AssocMap.lookup(ctx.txInfo.data)(datumHash) match
                      case Maybe.Just(d) => d
                      case Maybe.Nothing => throw new Exception("Unknown datum hash in the output")
                val operations = PreludeListData.listFromData[TrustRegistryOperation](datum)
                true
              case _ => false
          case _ => false
    }
    
    if (scalus.prelude.List.isEmpty(myOutputs)) then
      throw new Exception("No outputs with the given name")
    


  }



}
