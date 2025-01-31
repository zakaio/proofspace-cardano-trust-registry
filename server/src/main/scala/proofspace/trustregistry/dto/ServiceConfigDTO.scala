package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

case class CardanoWalletConfigDTO(
                                 hash: Option[String],
                                 seedPhrase: Option[String],
                                 )


case class CardanoServiceConfigDTO(
              testnet: Option[CardanoWalletConfigDTO] = None,
              mainnet: Option[CardanoWalletConfigDTO] = None,
                                  )

case class ServiceConfigDTO(
        cardano: CardanoServiceConfigDTO,
                           )

object ServiceConfigDTO {

  given JsonValueCodec[ServiceConfigDTO] = JsonCodecMaker.make

}
