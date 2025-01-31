package proofspace.platform.dto


import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

case class ProofSpaceServiceViewDTO(
                                     name: String,
                                     description: Option[String] = None,
                                     publicDid: String,
                                     enabled: Boolean,
                                     isPublic: Boolean,
                                     endpointsBase: String,
                                     owner: Option[String] = None,
                                   )

object ProofSpaceServiceViewDTO {

  given JsonValueCodec[ProofSpaceServiceViewDTO] = JsonCodecMaker.make

  given seqJsonValueCodec: JsonValueCodec[Seq[ProofSpaceServiceViewDTO]] = JsonCodecMaker.make

}

