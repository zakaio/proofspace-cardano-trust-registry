package proofspace.trustregistry.n2n

import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global
import cps.*
import cps.monads.{*, given}
import com.bloxbean.cardano.client.account.Account
import com.bloxbean.cardano.client.address.{Address, AddressProvider}
import com.bloxbean.cardano.client.common.model.Networks
import com.bloxbean.cardano.client.crypto.{KeyGenUtil, Keys, SecretKey, VerificationKey}
import com.bloxbean.cardano.client.crypto.bip39.{MnemonicCode, Words}
import com.bloxbean.cardano.client.util.HexUtil
import com.dimafeng.testcontainers.{ContainerDef, MongoDBContainer}
import com.dimafeng.testcontainers.munit.TestContainerForAll
import proofspace.trustregistry.dto.{CardanoContractDTO, CardanoContractGenerateDTO}
import sttp.client3.*
import sttp.client3.jsoniter.*
import sttp.client3.pekkohttp.*


import java.io.{File, FileOutputStream}


class TestCardanoTestNetContracts extends munit.FunSuite with TestContainerForAll with TrustRegistryFixturesWithContainer {



  override val containerDef: ContainerDef = MongoDBContainer.Def("mongo:8.0")

  val generateNewMnemonic = true

  def generateNewKey(keyname: String): Keys = {
    val keys = KeyGenUtil.generateKey()
    keys
  }

  def writeKeyData(keyname: String, keys: Keys): Unit = {
    val secretKey = keys.getSkey
    val verificaionKey = keys.getVkey
    writeToFile(s"src/test/resources/${keyname}.vkey", HexUtil.encodeHexString(verificaionKey.getBytes))
    writeToFile(s"src/test/resources/${keyname}.skey", HexUtil.encodeHexString(secretKey.getBytes))
    val pkh = KeyGenUtil.getKeyHash(verificaionKey)
    writeToFile(s"src/test/resources/${keyname}.pkh", pkh)
    //val address = AddressProvider.getEntAddress(pkh, Networks.testnet)
    //writeToFile(s"src/test/resources/${keyname}.address", address.toBech32)
  }

  def generateAddressWithMnemonic(keyname: String): Address = {
    val mnemonic = MnemonicCode.INSTANCE.createMnemonic(Words.TWENTY_FOUR)
    val mnemonicString = String.join(" ", mnemonic)
    val account = new Account(Networks.testnet(), mnemonicString)
    val pk = account.hdKeyPair().getPublicKey()
    val address = AddressProvider.getEntAddress(pk, Networks.testnet)
    writeToFile(s"src/test/resources/${keyname}.mnemonic", mnemonicString)
    writeToFile(s"src/test/resources/${keyname}.pkh", HexUtil.encodeHexString(pk.getKeyHash))
    writeToFile(s"src/test/resources/${keyname}.address", address.toBech32)
    address
  }

  def writeToFile(fname: String, data: String): Unit = {
    val file = new File(fname)
    //val hex = HexUtil.encodeHexString(data)
    val fos = new FileOutputStream(file)
    try
      fos.write(data.getBytes())
    finally
      fos.close()
  }

  def getOrGenerateKey(keyname: String): Keys = {
    val pkhFile = new File(s"src/test/resources/${keyname}.pkh")
    if (!pkhFile.exists()) {
        val keys = generateNewKey(keyname)
        writeKeyData(keyname, keys)
        keys
    } else {
        val hex = scala.io.Source.fromFile(pkhFile).getLines().mkString
        val pkhBytes = HexUtil.decodeHexString(hex)
        val privateKeyHex = scala.io.Source.fromFile(new File(s"src/test/resources/${keyname}.skey")).getLines().mkString
        val secretKey = SecretKey.create(HexUtil.decodeHexString(privateKeyHex))
        val publicKeyHex = scala.io.Source.fromFile(new File(s"src/test/resources/${keyname}.vkey")).getLines().mkString
        val verificationKey = VerificationKey.create(HexUtil.decodeHexString(publicKeyHex))
        new Keys(secretKey, verificationKey)
    }
  }

  def getOrGenerateAddress(keyname: String): Address = {
    val addressFile = new File(s"src/test/resources/${keyname}.address")
    val addressString = if (addressFile.exists()) {
      scala.io.Source.fromFile(addressFile).getLines().mkString
    } else {
      val address = generateAddressWithMnemonic(keyname)
      address.toBech32
    }
    new Address(addressString)
  }


  test("create SingleMaintainerContract") {
    val keyname = "test1_maintainer"
    val keys = getOrGenerateKey(keyname)
    val verificationKey = keys.getVkey
    val pkh = KeyGenUtil.getKeyHash(verificationKey)
    val f = async[Future] {
      val appConfig = await(serverFixture())
      val sttpBackend = sttpBackendFixture()
      val createTrustRegistryRequest = sttp.client3.basicRequest
        .post(uri"http://localhost:${appConfig.port}/cardano/script/from-template")
        .body(
          CardanoContractGenerateDTO(
            subnetwork = "testnet",
            contract = CardanoContractDTO(
              registryName = "cardano-single-maintainer-t1",
              templateName = "singleRegistryMaintainer",
              parameters = Seq(pkh)
            )
          )
        )
      val response = await(sttpBackend.send(createTrustRegistryRequest))
      println(s"response=${response}")
      assert(response.code.isSuccess)
    }
    f
  }

}
