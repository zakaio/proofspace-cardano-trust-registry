package proofspace.trustregistry.n2n

import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global
import cps.*
import cps.monads.{*, given}
import com.bloxbean.cardano.client.account.Account
import com.bloxbean.cardano.client.address.{Address, AddressProvider}
import com.bloxbean.cardano.client.common.model.{Network, Networks}
import com.bloxbean.cardano.client.crypto.{KeyGenUtil, Keys, SecretKey, VerificationKey}
import com.bloxbean.cardano.client.crypto.bip39.{MnemonicCode, Words}
import com.bloxbean.cardano.client.util.HexUtil
import com.dimafeng.testcontainers.{ContainerDef, MongoDBContainer}
import com.dimafeng.testcontainers.munit.TestContainerForAll
import proofspace.trustregistry.dto.CardanoCreateParams.Template
import proofspace.trustregistry.dto.{CardanoContractDTO, CardanoContractGenerateDTO, CreateTrustRegistryDTO}
import sttp.client3.*
import sttp.client3.jsoniter.*
import sttp.client3.pekkohttp.*

import java.io.{File, FileOutputStream}


class TestCardanoTestNetContracts extends munit.FunSuite with TrustRegistryFixturesWithContainer {


  val generateNewMnemonic = true

  def cardanoNetwork: String =
    if !(System.getenv(ENV_BLOCKFROST_NETWORK) eq null) then
      System.getenv(ENV_BLOCKFROST_NETWORK)
    else
      "testnet"


  def generateNewKey(keyname: String): Keys = {
    val keys = KeyGenUtil.generateKey()
    keys
  }

  def writeKeyData(keyname: String, keys: Keys): Unit = {
    val secretKey = keys.getSkey
    val verificaionKey = keys.getVkey
    CardanoUtils.writeToFile(s"src/test/resources/${keyname}.vkey", HexUtil.encodeHexString(verificaionKey.getBytes))
    CardanoUtils.writeToFile(s"src/test/resources/${keyname}.skey", HexUtil.encodeHexString(secretKey.getBytes))
    val pkh = KeyGenUtil.getKeyHash(verificaionKey)
    CardanoUtils.writeToFile(s"src/test/resources/${keyname}.pkh", pkh)
    //val address = AddressProvider.getEntAddress(pkh, Networks.testnet)
    //writeToFile(s"src/test/resources/${keyname}.address", address.toBech32)
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
            subnetwork = cardanoNetwork,
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

  test("create AnySubmitMaintainerApproveContract") {
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
            subnetwork = cardanoNetwork,
            contract = CardanoContractDTO(
              registryName = "cardano-maintainer-approve-t1",
              templateName = "submitWithCostMaintainerApprove",
              parameters = Seq(pkh,"1000000")
            )
          )
        )
      val response = await(sttpBackend.send(createTrustRegistryRequest))
      println(s"response=${response}")
      assert(response.code.isSuccess)
    }
    f

  }


  test("submit contract to blockchain") {
    val keyname = "test1_maintainer"
    val keys = getOrGenerateKey(keyname)
    val verificationKey = keys.getVkey
    val pkh = KeyGenUtil.getKeyHash(verificationKey)
    val test_blockfrost_api = System.getenv(ENV_BLOCKFROST_API)
    if ((test_blockfrost_api eq null) || test_blockfrost_api.isEmpty) then
      println("BLOCKFROST_API not set, skipping test")
      Future.successful(())
    else
      val f = async[Future] {
        val appConfig = await(serverFixture())
        val sttpBackend = sttpBackendFixture()
        val createTrustRegistryRequest = sttp.client3.basicRequest
          .post(uri"http://localhost:${appConfig.port}/trust-registry")
          .body(
            CreateTrustRegistryDTO(
              name = "test",
              network = "cardano",
              subnetwork = Some(cardanoNetwork),
              cardano = Some(
                Template(
                  CardanoContractDTO(
                    registryName = "cardano-maintainer-approve-t2",
                    templateName = "submitWithCostMaintainerApprove",
                    parameters = Seq(pkh,"1000000")
                  )
                )
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
