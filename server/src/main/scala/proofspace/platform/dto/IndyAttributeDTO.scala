package proofspace.platform.dto

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

case class IndyAttributeEnumValue(name: String, value: String, description: Option[String] = None, dateUnits:Option[Int] = None)

enum IndyAttributeSource {
  case QR, NFC
}

object IndyAttributeSource {
  given JsonValueCodec[IndyAttributeSource] = JsonCodecMaker.make[IndyAttributeSource](CodecMakerConfig.withDiscriminatorFieldName(None))
}

enum IndyAttributeType {
  case `text`, `date`, `phone`, `number`, `file`, `imageUrl`
}

object IndyAttributeType {
  given JsonValueCodec[IndyAttributeType] = JsonCodecMaker.make[IndyAttributeType](CodecMakerConfig.withDiscriminatorFieldName(None))
}

case class IndyAttributeDTO(
                             name: String,
                             description: Option[String] = None,
                             validatorRegexpr: Option[String] = None,
                             @named("type")
                             attrType: IndyAttributeType,
                             attrSource: Option[IndyAttributeSource] = None,
                             enumValues: List[IndyAttributeEnumValue] = List.empty,
                             dateUnits: Option[Int] = None)

object IndyAttributeDTO {
  given JsonValueCodec[IndyAttributeDTO] = JsonCodecMaker.make[IndyAttributeDTO]
}


