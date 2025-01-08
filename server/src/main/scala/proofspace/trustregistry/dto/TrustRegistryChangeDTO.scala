package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

case class TrustRegistryChangeDTO(
    trustRegistryId: String,
    changeId: Option[String]=None,
    addedDids: Seq[String] = Seq.empty,
    removedDids: Seq[String] = Seq.empty,                             
)

object TrustRegistryChangeDTO {
  
  given JsonValueCodec[TrustRegistryChangeDTO] = JsonCodecMaker.make
  
}
