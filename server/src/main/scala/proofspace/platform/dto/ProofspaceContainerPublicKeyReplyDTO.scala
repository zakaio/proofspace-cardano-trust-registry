package proofspace.platform.dto


import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

case class ProofspaceContainerPublicKeyReplyDTO(
                                                 val name: String,
                                                 val publicKey: String
                                               )

object ProofspaceContainerPublicKeyReplyDTO {
  given JsonValueCodec[ProofspaceContainerPublicKeyReplyDTO] = JsonCodecMaker.make
}


