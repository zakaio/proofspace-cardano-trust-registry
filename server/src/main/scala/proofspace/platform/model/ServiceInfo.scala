package proofspace.platform.model

import java.security.interfaces.RSAPublicKey
import java.time.OffsetDateTime

case class ServiceInfo(
                        proofspaceDid: String,
                        endpoint: String,
                        name: String,
                        enabled: Boolean,
                        defaultPublicKey: Option[RSAPublicKey],
                        updated: OffsetDateTime,
                        expires: OffsetDateTime
                      )
