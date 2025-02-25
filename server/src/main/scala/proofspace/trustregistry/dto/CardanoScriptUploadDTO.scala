package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

case class CardanoScriptUploadDTO(
                                 name: Option[String],
                                 doubleCborHex: String,
                                 )


object CardanoScriptUploadDTO {
  given JsonValueCodec[CardanoScriptUploadDTO] = JsonCodecMaker.make
}

case class CardanoScriptUploadResponseDTO(
                                 hashCodeHex: String,
                                 )

object CardanoScriptUploadResponseDTO {
  given JsonValueCodec[CardanoScriptUploadResponseDTO] = JsonCodecMaker.make
}