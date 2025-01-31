package proofspace.platform.util

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}

object AES {

  val KEY_SIZE = 32
  private val IV_SIZE = 16
  private val random: SecureRandom = new SecureRandom()

  def encrypt(encKey: Array[Byte], value: Array[Byte]): Array[Byte] = {
    val cipher = Cipher.getInstance ("AES/CBC/PKCS5Padding")
    val keySpec = SecretKeySpec(encKey, "AES")
    val iv = generateRandomBytes(IV_SIZE)
    val ivSpec = IvParameterSpec(iv)
    cipher.init (Cipher.ENCRYPT_MODE, keySpec, ivSpec);
    val encryptedValue = cipher.doFinal(value)
    val retval = new Array[Byte] (encryptedValue.size + IV_SIZE)
    System.arraycopy(iv,0, retval, 0, IV_SIZE)
    System.arraycopy(encryptedValue, 0, retval, IV_SIZE, encryptedValue.size)
    retval
  }

  def decrypt(encKey: Array[Byte], value: Array[Byte]): Array[Byte] = {
    val cipher = Cipher.getInstance ("AES/CBC/PKCS5Padding")
    val keySpec = SecretKeySpec(encKey, "AES")
    val iv = value.slice(0, IV_SIZE)
    val ivSpec = IvParameterSpec(iv)
    val clearValue = value.slice(IV_SIZE, value.size)
    cipher.init (Cipher.DECRYPT_MODE, keySpec, ivSpec)
    cipher.doFinal(clearValue)
  }

  def generateRandomBytes(nBytes: Int): Array[Byte] = {
    val retval = new Array[Byte] (nBytes)
    random.nextBytes(retval)
    retval
  }


}
