package proofspace.trustregistry.gateways.cardano

import com.bloxbean.cardano.client.common.model.{Networks, Network}

object CardanoHelper {

  def asNetwork(snetwork: String): Network = {
    snetwork match
      case "mainnet" => Networks.mainnet()
      case "testnet" => Networks.testnet()
      case "preprod" => Networks.preprod()
      case "preview" => Networks.preview()
      case _ => throw IllegalArgumentException(s"Invalid network $snetwork")
  }


}
