package proofspace.trustregistry.model

import scalus.builtin.Data.{FromData, ToData}
import scalus.builtin.{Builtins, ByteString, Data, ToData}
import scalus.ledger.api.v3.{*, given}
import scalus.builtin.ToDataInstances.given
import scalus.builtin.FromDataInstances.given

/**
 * Maintance model - is a way how the trust registry is maintained.
 */
enum TrustRegistryMaintanceModel {

  /**
   * Trust registry is defined by maintance model.
   * 
   */
  case SingleMaintainer(pkh: PubKeyHash, address: Address, flags: BigInt)

  /**
   * Trust registry is defined by owners of the token.
   * Minting or Burning of the token is the only way to change the trust registry.
   */
  case TokenOwnership(currencySymbol: CurrencySymbol, didMapping: DidTokenMapping, flags: BigInt)

  /**
   * Owner of tokens are allowed to vote on the changes of the trust registry.
   * The trust registry is changed by the voting.
   * Voting transactions are signed by the owners of the tokens and send to collect Address.
   */
  case VotingTokenOwnership(currencySymbol: CurrencySymbol, collectAddress: Credential, flags: BigInt)


}

object TrustRegistryMaintanceModel {

  given ToData[TrustRegistryMaintanceModel] = (m: TrustRegistryMaintanceModel) => m match {
    case TrustRegistryMaintanceModel.SingleMaintainer(pkh, addr,  flags) =>
      Builtins.constrData(0, 
        scalus.builtin.List(
          summon[ToData[PubKeyHash]](pkh),
          summon[ToData[Address]](addr),
          Builtins.iData(flags)))
    case TrustRegistryMaintanceModel.TokenOwnership(currencySymbol, didMapping, flags) =>
      Builtins.constrData(1, scalus.builtin.List(
        Data.B(currencySymbol),
        summon[ToData[DidTokenMapping]](didMapping),
        Builtins.iData(flags)
      ))
    case TrustRegistryMaintanceModel.VotingTokenOwnership(currencySymbol, collectAddress, flags) =>
      Builtins.constrData(2, scalus.builtin.List(
        summon[ToData[CurrencySymbol]](currencySymbol),
        summon[ToData[Credential]](collectAddress),
        Builtins.iData(flags)
      ))
  }

  given FromData[TrustRegistryMaintanceModel] = (data) => {
    val scalus.builtin.Pair(constr, args) = Builtins.unConstrData(data)
    constr match {
      case 0 =>
        val pkh = summon[FromData[PubKeyHash]](args.head)
        val argsTail = args.tail
        val addr = summon[FromData[Address]](argsTail.head)
        val flags = Builtins.unIData(argsTail.tail.head)
        TrustRegistryMaintanceModel.SingleMaintainer(pkh, addr, flags)
      case 1 =>
        val currencySymbol = summon[FromData[CurrencySymbol]](args.head)
        val argsTail = args.tail
        val didMapping = summon[FromData[DidTokenMapping]](argsTail.head)
        val flags = Builtins.unIData(argsTail.tail.head)
        TrustRegistryMaintanceModel.TokenOwnership(currencySymbol, didMapping, flags)
      case 2 =>
        val currencySymbol = summon[FromData[CurrencySymbol]](args.head)
        val collectAddress = summon[FromData[Credential]](args.tail.head)
        val flags = Builtins.unIData(args.tail.tail.head)
        TrustRegistryMaintanceModel.VotingTokenOwnership(currencySymbol, collectAddress, flags)
    }
  }


}


enum DidTokenMapping {
  case TokenName
  case MetadataField(metadataField: ByteString, prefix: ByteString)
}

object DidTokenMapping {

  given ToData[DidTokenMapping] = (m: DidTokenMapping) => m match {
    case DidTokenMapping.TokenName =>
      Builtins.constrData(0, scalus.builtin.List())
    case DidTokenMapping.MetadataField(metadataField, prefix) =>
      Builtins.constrData(1, scalus.builtin.List(Data.B(metadataField), Data.B(prefix))
      )
  }

  given FromData[DidTokenMapping] = (data) => {
    val scalus.builtin.Pair(constr, args) = Builtins.unConstrData(data)
    constr match {
      case 0 => DidTokenMapping.TokenName
      case 1 =>
        val metadataField =  args.head.toByteString
        val prefix = args.tail.head.toByteString
        DidTokenMapping.MetadataField(metadataField, prefix)
    }
  }

}