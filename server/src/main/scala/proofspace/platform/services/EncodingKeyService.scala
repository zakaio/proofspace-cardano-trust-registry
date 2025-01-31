package proofspace.platform.services

import org.slf4j.LoggerFactory
import proofspace.platform.model.EncKeysConfig
import proofspace.platform.util.AES
import proofspace.trustregistry.AppConfig


class EncodingKeyService(config: AppConfig) {

  private val logger = LoggerFactory.getLogger(classOf[EncodingKeyService])

  private val keys = EncKeysConfig.read(config.encodingKeyFile)
  private val encKeys: Map[String, Array[Byte]] = keys.keys.map(x => makeKey(x._1, x._2)).toMap
  private val encKeyNames = keys.keys.map(x => x._1).toArray
  private val random = new scala.util.Random

  def randomKeyName: String = {
    println(s"randomKeyName,  encKeyNames = ${encKeyNames},  length = ${encKeyNames.length}")
    val randomIndex = random.nextInt(encKeyNames.length)
    encKeyNames(randomIndex)
  }

  def getKey(keyName: String): Array[Byte] = {
    encKeys.get(keyName) match {
      case Some(key) => key
      case None => throw new IllegalArgumentException(s"Key with name $keyName not found")
    }
  }

  def firstTimeEncode(value: Array[Byte]): (String, Array[Byte]) = {
    val keyName = randomKeyName
    val key = getKey(keyName)
    val encValue = AES.encrypt(key, value)
    (keyName, encValue)
  }

  def encode(keyName: String, value: Array[Byte]): Array[Byte] = {
    val key = getKey(keyName)
    AES.encrypt(key, value)
  }

  def decode(keyName: String, value: Array[Byte]): Array[Byte] = {
    val key = getKey(keyName)
    println(s"before AES.decrypt, keyName = ${keyName}, value = ${value}m key = ${key}, keySize=${key.length}")
    AES.decrypt(key, value)
  }

  private def makeKey(keyName: String, key: String): (String, Array[Byte]) = {
    var keyBytes = key.getBytes("UTF-8")
    if (keyBytes.length < 32) {
      //fill to 32 bytes
      logger.warn("Key {} is too short, filling to 32 bytes", keyName)
      val newKeyBytes = new Array[Byte](32)
      System.arraycopy(keyBytes, 0, newKeyBytes, 0, keyBytes.length)
      keyBytes = newKeyBytes
    } else if (keyBytes.length > 32) {
      //truncate to 32 bytes
      logger.warn("Key {} is too long, truncating to 32 bytes", keyName)
      keyBytes = keyBytes.slice(0, 32)
    }
    (keyName, keyBytes)
  }

}


