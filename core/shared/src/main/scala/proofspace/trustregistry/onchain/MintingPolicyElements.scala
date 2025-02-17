package proofspace.trustregistry.onchain

import proofspace.trustregistry.model.{TrustRegistryDatum, TrustRegistryOperation}
import scalus.Compile
import scalus.builtin.ByteString
import scalus.builtin.Data.FromData
import scalus.ledger.api.v2.OutputDatum
import scalus.ledger.api.v3.{Datum, PubKeyHash, ScriptContext, ScriptInfo, TxInfo, TxOut}
import scalus.prelude.{AssocMap, Maybe}
import scalus.prelude.Prelude.{*, given}

@Compile
object MintingPolicyElements {

  /**
   * Check, if the given public key hash is verified by the maintainer.
   * We assume that changes in the trust registry are packet in the transaction,
   * but do not verify this.
   */
  def verifyPkh(pkh: PubKeyHash)(ctx: ScriptContext) = {
    scalus.prelude.List.findOrFail(ctx.txInfo.signatories)(_ === pkh)
  }

  /**
   * Retrieve datum from the output, fail if it is absent.
   */
  def retrieveDatum(txOut: TxOut, txInfo: TxInfo): Datum = {
    txOut.datum match
      case OutputDatum.NoOutputDatum => throw new Exception("No datum in the output")
      case OutputDatum.OutputDatum(d) => d
      case OutputDatum.OutputDatumHash(datumHash) =>
        AssocMap.lookup(txInfo.data)(datumHash) match
          case Maybe.Just(d) => d
          case _ => throw new Exception("Unknown datum hash in the output")
  }

  def filterMinted(ctx: ScriptContext, registryName: ByteString,
                   checkOps: (TxOut, TrustRegistryDatum, scalus.prelude.List[TrustRegistryOperation]) => Boolean,
                   checkOtherOut: TxOut => Unit = _ => (),
                  ): scalus.prelude.List[TxOut] = {
    val ownSym = ctx.scriptInfo match
      case ScriptInfo.MintingScript(sym) => sym
      case _ => throw new Exception("Minting script is expected")
    val txInfo = ctx.txInfo
    scalus.prelude.List.filter(txInfo.outputs){
      txOut =>
        AssocMap.lookup(txOut.value)(ownSym) match
          case Maybe.Just(byNames) =>
            AssocMap.lookup(byNames)(registryName) match
              case Maybe.Just(v) =>
                val datum = retrieveDatum(txOut, txInfo)
                val parsedDatum = summon[FromData[TrustRegistryDatum]](datum)
                val ops = parsedDatum match
                  case TrustRegistryDatum.Operations(ops) => ops
                  case TrustRegistryDatum.SeeReferenceIndex(idx) =>
                    val referenceInput = scalus.prelude.List.getByIndex(txInfo.referenceInputs)(idx)
                    val refDatum = retrieveDatum(referenceInput.resolved, txInfo)
                    summon[FromData[TrustRegistryDatum]](refDatum) match
                      case TrustRegistryDatum.Operations(ops) => ops
                      case _ => throw new Exception("Invalid datum in the reference input")
                  case TrustRegistryDatum.SeeNormalInput(idx) =>
                    val normalInput = scalus.prelude.List.getByIndex(txInfo.inputs)(idx)
                    val normalDatum = retrieveDatum(normalInput.resolved, txInfo)
                    summon[FromData[TrustRegistryDatum]](normalDatum) match
                      case TrustRegistryDatum.Operations(ops) => ops
                      case _ => throw new Exception("Invalid datum in the normal input")
                if (scalus.prelude.List.isEmpty(ops))
                  throw new Exception("No operations in the output")
                else
                  checkOps(txOut, parsedDatum, ops)
              case _ =>
                throw new Exception("Output for the other registry should not be minted in this transaction")
          case _ =>
            checkOtherOut(txOut)
            false
    }
  }

}
