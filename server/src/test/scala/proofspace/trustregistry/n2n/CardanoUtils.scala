package proofspace.trustregistry.n2n

import com.bloxbean.cardano.client.account.Account
import com.bloxbean.cardano.client.address.{Address, AddressProvider}
import com.bloxbean.cardano.client.common.model.{Network, Networks}
import com.bloxbean.cardano.client.crypto.bip39.{MnemonicCode, Words}
import com.bloxbean.cardano.client.util.HexUtil

import java.io.{File, FileOutputStream}

object CardanoUtils {


  def asBloxbeanNetwork(name: String): Network =
    if (name == "testnet") then
      Networks.testnet()
    else if (name == "preprod") then
      Networks.preprod()
    else if (name == "preview") then
      Networks.preview()
    else if (name == "mainnet") then
      Networks.mainnet()
    else
      throw IllegalArgumentException(s"Unknown network $name")


  def generateAddressWithMnemonic(keyname: String, cardanoNetwork: String): (Address, String) = {
    val mnemonic = MnemonicCode.INSTANCE.createMnemonic(Words.TWENTY_FOUR)
    val mnemonicString = String.join(" ", mnemonic)
    val network = asBloxbeanNetwork(cardanoNetwork)
    val account = new Account(network, mnemonicString)
    val pk = account.hdKeyPair().getPublicKey()
    val address = AddressProvider.getEntAddress(pk, network)
    writeToFile(s"src/test/resources/${keyname}.mnemonic", mnemonicString)
    writeToFile(s"src/test/resources/${keyname}.pkh", HexUtil.encodeHexString(pk.getKeyHash))
    writeToFile(s"src/test/resources/${keyname}.address", address.toBech32)
    (address, mnemonicString)
  }


  def writeToFile(fname: String, data: String): Unit = {
    val file = new File(fname)
    val fos = new FileOutputStream(file)
    try
      fos.write(data.getBytes())
    finally
      fos.close()
  }

  def getOrGenerateAddress(keyname: String, cardanoNetwork: String): (Address, String) = {
    val addressFile = new File(s"src/test/resources/${keyname}.address")
    val mnemonicFile = new File(s"src/test/resources/${keyname}.mnemonic")
    val (address, mnemonic) = if (addressFile.exists()) {
      val addressSting = scala.io.Source.fromFile(addressFile).getLines().mkString
      val mnemonic = scala.io.Source.fromFile(mnemonicFile).getLines().mkString
      (new Address(addressSting), mnemonic)
    } else {
      generateAddressWithMnemonic(keyname, cardanoNetwork)
    }
    (address, mnemonic)
  }


}
