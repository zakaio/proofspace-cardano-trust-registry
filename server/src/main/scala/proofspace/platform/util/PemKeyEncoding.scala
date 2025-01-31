package proofspace.platform.util

import org.bouncycastle.util.io.pem.{PemObject, PemReader, PemWriter}
import org.slf4j.LoggerFactory
import proofspace.trustregistry.controllers.HttpException
import sttp.model.StatusCode

import java.io.{OutputStreamWriter, StringReader, StringWriter}
import java.security.KeyFactory
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import scala.io.Source
import scala.util.control.NonFatal

object PemKeyEncoding {

  private val logger = LoggerFactory.getLogger(classOf[PemKeyEncoding.type])

  def readRSAPrivateKeyFromFile(fname: String): RSAPrivateKey = {
    val source = Source.fromFile(fname)
    try
      val pem = source.mkString
      parseRSAPrivateKey(pem)
    catch
      case NonFatal(e) =>
        logger.error(s"Ca't read RSA private key from file $fname", e)
        throw HttpException(StatusCode.InternalServerError, "Can't read RSA private key from file", isLogged = true)
    finally
      source.close()
  }

  def readRSAPublicKeyFromFile(fname: String): RSAPublicKey = {
    val source = Source.fromFile(fname)
    try
      val pem = source.mkString
      parseRSAPublicKey(pem)
    catch
      case NonFatal(e) =>
        logger.error(s"Can't read RSA public key from file $fname", e)
        throw HttpException(StatusCode.InternalServerError, "Can't read RSA public key from file", isLogged = true)
    finally
      source.close()
  }

  /**
   * Parse a PEM encoded RSA public key or throw exception
   *
   * @param pem
   * @return
   */
  def parseRSAPublicKey(pem: String): RSAPublicKey = {
    val pemReader = new PemReader(new StringReader(pem));
    val pemObject = pemReader.readPemObject();
    val bytes = pemObject.getContent()
    val keySpec = new X509EncodedKeySpec(bytes)
    val keyFactory = KeyFactory.getInstance("RSA")
    val retval = keyFactory.generatePublic(keySpec)
    if (!retval.isInstanceOf[RSAPublicKey]) {
      throw new IllegalArgumentException("Not an RSA public key")
    } else
      retval.asInstanceOf[RSAPublicKey]
  }

  def parseRSAPrivateKey(pem: String): RSAPrivateKey = {
    val pemReader = new PemReader(new StringReader(pem));
    val pemObject = pemReader.readPemObject();
    val bytes = pemObject.getContent()
    val privKeySpec = new PKCS8EncodedKeySpec(bytes)
    val keyFactory = KeyFactory.getInstance("RSA")
    val retval = keyFactory.generatePrivate(privKeySpec).asInstanceOf[RSAPrivateKey]
    if (!retval.isInstanceOf[RSAPrivateKey]) {
      throw new IllegalArgumentException("Not an RSA private key")
    } else
      retval.asInstanceOf[RSAPrivateKey]
  }

  def writeRSAPrivateKey(key: RSAPrivateKey): String = {
    val keyFactory = KeyFactory.getInstance("RSA")
    val keySpec = keyFactory.getKeySpec(key, classOf[PKCS8EncodedKeySpec])
    val writer = new StringWriter()
    val pemWriter = new PemWriter(writer)
    pemWriter.writeObject(new PemObject("RSA PRIVATE KEY", keySpec.getEncoded))
    pemWriter.close()
    writer.toString
  }


}
