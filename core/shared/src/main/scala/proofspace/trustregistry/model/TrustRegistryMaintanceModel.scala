package proofspace.trustregistry.model

import scalus.ledger.api.v3.{Credential, CurrencySymbol, PubKeyHash}

/**
 * Maintance model - is a way how the trust registry is maintained.
 */
enum TrustRegistryMaintanceModel {

  /**
   * Trust registry is defined by maintance model.
   * 
   */
  case SingleMaintainer(pkh: PubKeyHash)

  /**
   * Trust registry is defined by owners of the token.
   * Minting or Burning of the token is the only way to change the trust registry.
   */
  case TokenOwnership(currencySymbol: CurrencySymbol, didMapping: DidTokenMapping)

  /**
   * Owner of tokens are allowed to vote on the changes of the trust registry.
   * The trust registry is changed by the voting.
   * Voting transactions are signed by the owners of the tokens and send to collect Address.
   */
  case VotingTokenOwnership(currencySymbol: CurrencySymbol, collectAddress: Credential)

}


enum DidTokenMapping {
  case TokenName
  case MetadataField(metadataField: String, prefix: String)
}