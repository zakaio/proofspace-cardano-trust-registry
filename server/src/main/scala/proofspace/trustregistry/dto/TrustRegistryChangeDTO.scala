package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import java.time.OffsetDateTime

case class InRegistryChangeDTO(
                                addedDids: Seq[String] = Seq.empty,
                                removedDids: Seq[String] = Seq.empty,
)

object InRegistryChangeDTO {
  
  given JsonValueCodec[InRegistryChangeDTO] = JsonCodecMaker.make
  
}

case class TrustRegistryChangeDTO(
                                   registryId: String,
                                   changeId: Option[String]=None,
                                   addedDids: Seq[String] = Seq.empty,
                                   removedDids: Seq[String] = Seq.empty,
                                   approved: Option[Boolean] = None,
                                   changeDate: Option[OffsetDateTime] = None,
                                   transactionId: Option[String] = None,
)

object TrustRegistryChangeDTO {
  
  given JsonValueCodec[TrustRegistryChangeDTO] = JsonCodecMaker.make
  
}

case class TrustRegistryChangesDTO(
                                  items: Seq[TrustRegistryChangeDTO],
                                  itemsTotal: Int
                                  )

object TrustRegistryChangesDTO {

  given JsonValueCodec[TrustRegistryChangesDTO] = JsonCodecMaker.make

}