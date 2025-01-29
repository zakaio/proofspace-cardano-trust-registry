package proofspace.trustregistry.util

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

object JSoniterDefaultCodecs {
  
  given JsonValueCodec[Boolean] = JsonCodecMaker.make
  
  
}
