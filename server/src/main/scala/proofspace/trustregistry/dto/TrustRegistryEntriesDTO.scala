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


enum TrustRegistryProposalStatusDTO {
  case Add
  case Remove
}

object TrustRegistryProposalStatusDTO {

  def toInt(status: TrustRegistryProposalStatusDTO): Int = status match {
    case TrustRegistryProposalStatusDTO.Add => 1
    case TrustRegistryProposalStatusDTO.Remove => 2
  }

  def fromInt(status: Int): TrustRegistryProposalStatusDTO = status match {
    case 1 => TrustRegistryProposalStatusDTO.Add
    case 2 => TrustRegistryProposalStatusDTO.Remove
    case _ => throw new IllegalArgumentException(s"Invalid Value for TrustRegistryProposalStatusDTO : $status")
  }

  given JsonValueCodec[TrustRegistryProposalStatusDTO] = JsonCodecMaker.make
}


case class TrustRegistryDidChangeDTO(
                                      changeId: String,
                                      status: TrustRegistryProposalStatusDTO,
                                      changeDate: OffsetDateTime
                                    )

/**
 * @param did - did of the issuer.  Note, that this can be external did.
 * @param state - state of the entry
 * @param lastChangeDate - last changed date in UTC timezone
 */
case class TrustRegistryEntryDTO(
                                  did: String,
                                  status: TrustRegistryEntryStatusDTO,
                                  acceptedChange: Option[TrustRegistryDidChangeDTO],
                                  proposedChange: Option[TrustRegistryDidChangeDTO],
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
