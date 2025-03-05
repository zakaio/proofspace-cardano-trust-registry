package proofspace.trustregistry.n2n

import com.bloxbean.cardano.client.address.Address
import com.bloxbean.cardano.client.crypto.bip39.MnemonicCode
import com.bloxbean.cardano.client.util.HexUtil
import com.dimafeng.testcontainers.{ContainerDef, MongoDBContainer}
import com.dimafeng.testcontainers.munit.TestContainerForAll
import munit.FutureFixture
import org.slf4j.LoggerFactory
import proofspace.trustregistry.{AppConfig, CardanoConfig, CardanoKeyConfig, CardanoNetworkConfig, ExternalServiceConfig, ProofspaceConfig, ProofspaceNetworkConfig, TrustRegistryServer}
import sttp.capabilities.WebSockets
import sttp.capabilities.pekko.PekkoStreams
import sttp.client3.SttpBackend
import sttp.client3.pekkohttp.PekkoHttpBackend

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

trait TrustRegistryFixturesWithContainer extends munit.FunSuite with TestContainerForAll {

  val ENV_BLOCKFROST_API = "BLOCKFROST_API"
  val ENV_BLOCKFROST_PROJECT = "BLOCKFROST_PROJECT"
  val ENV_BLOCKFROST_NETWORK = "BLOCKFROST_NETWORK"
  val ENV_BLOCKFFOST_KEY = "BLOCKFROST_KEY"


  class TrustRegistryFixture extends FutureFixture[Future[AppConfig]]("trustregistry-server") {

    private val appConfigPromise: Promise[AppConfig] = Promise[AppConfig]()
    private val trustRegistryPromise: Promise[TrustRegistryServer] = Promise[TrustRegistryServer]()
    private var initCalled = false
    private var beforeCreatingServer = false
    private var afterGetEnv = false
    private var server: TrustRegistryServer = null

    private val logger = LoggerFactory.getLogger(classOf[TrustRegistryFixture])


    def init(container: MongoDBContainer): Unit = {
      initCalled = true
      val blockfrost_api = System.getenv(ENV_BLOCKFROST_API)
      afterGetEnv = true
      val blockfrost_project = System.getenv(ENV_BLOCKFROST_PROJECT)
      val blockfrost_network = System.getenv(ENV_BLOCKFROST_NETWORK)
      val blockfrost_key = System.getenv(ENV_BLOCKFFOST_KEY)
      val testCardanoNetwork =
        if (blockfrost_network eq null) || (blockfrost_network.isEmpty) then
          "testnet"
        else
          blockfrost_network
      val testCardanoParams =
        if (blockfrost_network eq null) || (blockfrost_network.isEmpty) then
          logger.info("blockfrost network not set")
          CardanoConfig.default.subnetworks.get(testCardanoNetwork) match
            case Some(networkConfig) => networkConfig
            case None =>
              logger.warn(s"Cardano network ${blockfrost_network} not found")
              val ex = new IllegalArgumentException(s"Cardano network $blockfrost_network not found")
              trustRegistryPromise.failure(ex)
              throw ex
        else
          CardanoNetworkConfig(
            blockfrostUrl = blockfrost_api,
            blockfrostProjectId = blockfrost_project,
            blockfrostApiKey = blockfrost_key
          )
      val cardanoConfig = CardanoConfig(
        subnetworks = Map(
          testCardanoNetwork -> testCardanoParams
        )
      )
      val testCardanoKeyConfig =
        if (blockfrost_network eq null) || (blockfrost_network.isEmpty) then
          CardanoKeyConfig.default
        else
          val (testCardanoAddress, testMnemonic) = CardanoUtils.getOrGenerateAddress("test_service_wallet", testCardanoNetwork)
          val pkh = HexUtil.encodeHexString(testCardanoAddress.getPaymentCredentialHash.get())
          println(s"pkh =${pkh} ")
          CardanoKeyConfig(
            address = Some(testCardanoAddress.toBech32),
            hash = Some(pkh),
            seedPhrase = Some(testMnemonic)
          )

      val config = AppConfig(
        mongoUri = container.container.getConnectionString,
        mongoDbName = "test",
        cardano = cardanoConfig,
        proofspace = ProofspaceConfig(
          defaultNetwork = "test",
          networks = Map(
            "test" -> ProofspaceNetworkConfig(
              baseZakaUrl = "https://test.proofspace.id/zaka",
              requestSigning = None,
              defaultServiceDid = Some("testServiceDid") // TODO: get real service.
            )
          )
        ),
        externalServices = Map(
          "testServiceDid" -> ExternalServiceConfig(
            cardanoKeys = Map(
              testCardanoNetwork -> testCardanoKeyConfig
            )
          )
        )
      )
      println("creating server")
      logger.info("creating server")
      beforeCreatingServer = true
      server = new TrustRegistryServer()
      println("server created")
      trustRegistryPromise.success(server)
      server.start(config).onComplete {
        case scala.util.Success(started) =>
          if started then
            appConfigPromise.success(config)
          else
            appConfigPromise.failure(new RuntimeException("Failed to start TrustRegistryServer"))
        case scala.util.Failure(exception) => appConfigPromise.failure(exception)
      }
    }

    override def apply(): Future[AppConfig] = appConfigPromise.future

    override def afterAll(): Future[Unit] = {
      println("TrustRegistryFixture.afterAll")
      super.afterAll().flatMap { _ =>
        println(s"super.afterAll, ${trustRegistryPromise.future}, initCalled=${initCalled}, beforeCreatingServer=${beforeCreatingServer}, afterGetEnv=${afterGetEnv}")
        if (initCalled) {
          if (server != null) {
            println(s"server = ${server}")
          } else {
            println("server is null")
          }
          trustRegistryPromise.future.flatMap {
            trustRegistryServer =>
              println("finishing server")
              trustRegistryServer.finish().map(_ => ())
          }
        } else {
            Future.successful(())
        }
      }
    }


  }

  val serverFixture = new TrustRegistryFixture()

  class SttpBackendFixture extends FutureFixture[SttpBackend[Future, Any]]("sttp-backend") {
    private var backend: SttpBackend[Future, PekkoStreams with WebSockets] = null;

    override def apply(): SttpBackend[Future, PekkoStreams with WebSockets] = {
      backend
    }

    override def beforeAll(): Future[Unit] = {
      super.beforeAll().map{ _ =>
        backend = PekkoHttpBackend()
      }
    }

    override def afterAll(): Future[Unit] = {
      super.afterAll().flatMap(_ => backend.close())
    }

  }

  val sttpBackendFixture = new SttpBackendFixture()

  override val containerDef: ContainerDef = MongoDBContainer.Def("mongo:8.0")

  override val munitFixtures = List(serverFixture, sttpBackendFixture)

  override def afterContainersStart(container: containerDef.Container): Unit = {
    println("afterContainersStart")
    super.afterContainersStart(container)
    serverFixture.init(container.asInstanceOf[MongoDBContainer])
  }


  override def beforeAll(): Unit = {
    super.beforeAll()
    val p = java.security.Security.addProvider(
      new org.bouncycastle.jce.provider.BouncyCastleProvider()
    );
  }




}

