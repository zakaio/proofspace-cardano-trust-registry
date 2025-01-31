package proofspace.platform.dto

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

object TrustLevel {
  opaque type Value = Int

  def toValue(v: Int): Value = {
    if (v < 0 || v > 2) throw new IllegalArgumentException("TrustLevel value must be between 0 and 2")
    v
  }

  def fromValue(v: Value): Int = v

  final val SELF_ATTESTED: Value = 0
  final val SERVICE_ATTESTED: Value = 1
  final val TRUSTED_SERVICE_ATTESTED: Value = 2

  implicit val jsonValueCodec: JsonValueCodec[Value] = JsonCodecMaker.make
}


