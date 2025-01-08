package proofspace.trustregistry.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import java.time.{LocalDateTime, OffsetDateTime}

enum TrustRegistryEntryStatusDTO {
  case Active
  case Candidate
  case Withdrawn
  case WithdrawnCandidate
}

object TrustRegistryEntryStatusDTO {
  given JsonValueCodec[TrustRegistryEntryStatusDTO] = JsonCodecMaker.make
}

/**
 * @param did - did of the issuer.  Note, that this can be external did.
 * @param state - state of the entry
 * @param lastChangeDate - last changed date in UTC timezone
 */
case class TrustRegistryEntryDTO(
                                  did: String,
                                  state: TrustRegistryEntryStatusDTO,
                                  lastChangeId: Option[String],
                                  lastChangeDate: LocalDateTime,  
                                )  {

  given JsonValueCodec[TrustRegistryEntryDTO] = JsonCodecMaker.make

}

case class TrustRegistryEntriesDTO(
              items: Seq[TrustRegistryEntryDTO],
              itemsTotal: Int
                                  )


object TrustRegistryEntriesDTO {

  given JsonValueCodec[TrustRegistryEntriesDTO] = JsonCodecMaker.make

}
