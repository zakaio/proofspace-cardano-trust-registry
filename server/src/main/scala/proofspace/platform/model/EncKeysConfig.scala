package proofspace.platform.model

import metaconfig.{ConfDecoder, ConfEncoder, Hocon, Input, generic}

import java.io.File

case class EncKeysConfig(
                          keys: Map[String, String]
                        )

object EncKeysConfig {

  private val dummyLocalConfig = EncKeysConfig(Map.empty)

  implicit lazy val surface: generic.Surface[EncKeysConfig] = generic.deriveSurface
  implicit lazy val decoder: ConfDecoder[EncKeysConfig] = generic.deriveDecoder(dummyLocalConfig)
  implicit lazy val encoder: ConfEncoder[EncKeysConfig] = generic.deriveEncoder

  def read(fname: String):EncKeysConfig = {
    val file = new File(fname)
    if (!file.exists()) {
      throw new IllegalArgumentException(s"File $fname does not exist")
    }
    val input = Input.File(file)
    val retval = Hocon.fromInput(input).get.as[EncKeysConfig].get
    retval
  }

}

