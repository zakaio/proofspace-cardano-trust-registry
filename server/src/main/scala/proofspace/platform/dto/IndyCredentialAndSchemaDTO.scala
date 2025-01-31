package proofspace.platform.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

case class IndySchemaDTO(name: String,
                         schemaId: String,
                         ownerDid: String,
                         version: String,
                         attributes: List[IndyAttributeDTO],
                         isPublic: Boolean,
                         contextUri: Option[String] = None,
                         w3vcTemplate: Option[String] = None,
                         ownerNameVersion: Option[String] = None)

case class IndyCredentialDTO(credentialId: String, ownerDid: String, schemaId: String, trustLevel: TrustLevel.Value)

case class IndyCredentialAndSchemaDTO(credential: IndyCredentialDTO, schema: IndySchemaDTO)

object IndyCredentialAndSchemaDTO {
  implicit val jsonValueCodec: JsonValueCodec[IndyCredentialAndSchemaDTO] = JsonCodecMaker.make
}

