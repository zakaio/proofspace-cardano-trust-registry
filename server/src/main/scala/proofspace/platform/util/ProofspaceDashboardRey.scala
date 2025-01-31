package proofspace.platform.util


import metaconfig.*

import java.security.interfaces.RSAPrivateKey

sealed trait ProofspaceDashboardKey

object ProofspaceDashboardKey {
  case class SharedSecret(value: String) extends ProofspaceDashboardKey

  case class RSAKey(value: RSAPrivateKey) extends ProofspaceDashboardKey


  given ConfDecoder[ProofspaceDashboardKey] with {
    def read(conf: Conf): Configured[ProofspaceDashboardKey] = {
      conf.get[String]("type") match
        case Configured.Ok("shared-secret") =>
          conf.as[SharedSecret]
        case Configured.Ok("rsa-key") =>
          conf.as[RSAKey]
        case Configured.Ok(other) =>
          Configured.error(s"Invalid key type: $other")
        case Configured.NotOk(err) =>
          Configured.NotOk(err)
    }
  }

  given ConfEncoder[ProofspaceDashboardKey] with {
    def write(key: ProofspaceDashboardKey): Conf = {
      key match {
        case SharedSecret(value) =>
          Conf.Obj("type" -> Conf.Str("shared-secret"), "value" -> Conf.Str(value))
        case RSAKey(value) =>
          Conf.Obj("type" -> Conf.Str("rsa-key"), "value" -> Conf.Str(PemKeyEncoding.writeRSAPrivateKey(value)))
      }
    }
  }

  given ConfDecoder[SharedSecret] with {
    def read(conf: Conf): Configured[SharedSecret] = {
      conf.get[String]("value").map(SharedSecret(_))
    }
  }

  given ConfDecoder[RSAKey] with {
    def read(conf: Conf): Configured[RSAKey] = {
      conf.get[String]("value") match
        case Configured.Ok(pem) =>
          val key = PemKeyEncoding.parseRSAPrivateKey(pem)
          Configured.Ok(RSAKey(key))
        case Configured.NotOk(err) =>
          conf.get[String]("file").map { path =>
            val key = PemKeyEncoding.readRSAPrivateKeyFromFile(path)
            RSAKey(key)
          }
    }
  }

}

