package proofspace.trustregistry.n2n

import com.dimafeng.testcontainers.{ContainerDef, MongoDBContainer}
import com.dimafeng.testcontainers.munit.TestContainerForAll
import munit.FutureFixture
import proofspace.trustregistry.{AppConfig, CardanoConfig, ProofspaceConfig, ProofspaceNetworkConfig, TrustRegistryServer}
import sttp.capabilities.WebSockets
import sttp.capabilities.pekko.PekkoStreams
import sttp.client3.SttpBackend
import sttp.client3.pekkohttp.PekkoHttpBackend

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

trait TrustRegistryFixturesWithContainer extends munit.FunSuite with TestContainerForAll {

  class TrustRegistryFixture extends FutureFixture[Future[AppConfig]]("trustregistry-server") {

    private val appConfigPromise: Promise[AppConfig] = Promise[AppConfig]()
    private val trustRegistryPromise: Promise[TrustRegistryServer] = Promise[TrustRegistryServer]()


    def init(container: MongoDBContainer): Unit = {
      val config = AppConfig(
        mongoUri = container.container.getConnectionString,
        mongoDbName = "test",
        cardano = CardanoConfig.default,
        proofspace = ProofspaceConfig(
          defaultNetwork = "test",
          networks = Map(
            "test" -> ProofspaceNetworkConfig(
              baseZakaUrl = "https://test.proofspace.id/zaka",
              requestSigning = None,
              defaultServiceDid = Some("testServiceDid") // TODO: get real service.
            )
          )
        )
      )
      val server = new TrustRegistryServer()
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
        trustRegistryPromise.future.flatMap { trustRegistryServer =>
          trustRegistryServer.finish().map(_ => ())
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
      super.beforeAll()
      backend = PekkoHttpBackend()
      Future.successful(())
    }

    override def afterAll(): Future[Unit] = {
      super.afterAll().flatMap(_ => backend.close())
    }

  }

  val sttpBackendFixture = new SttpBackendFixture()

  override val containerDef: ContainerDef = MongoDBContainer.Def("mongo:8.0")

  override val munitFixtures = List(serverFixture, sttpBackendFixture)

  override def afterContainersStart(container: containerDef.Container): Unit = {
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
