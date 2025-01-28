package proofspace.trustregistry.n2n

import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global
import cps.*
import cps.monads.{*, given}
import com.dimafeng.testcontainers.{ContainerDef, MongoDBContainer}
import com.dimafeng.testcontainers.munit.TestContainerForAll
import munit.FutureFixture
import proofspace.trustregistry.*
import proofspace.trustregistry.dto.*
import sttp.capabilities.WebSockets
import sttp.capabilities.pekko.PekkoStreams
import sttp.client3.*
import sttp.client3.jsoniter.*
import sttp.client3.pekkohttp.*


class TestCreateAndEditLocalTrustRegistry extends munit.FunSuite with TestContainerForAll {

  override val containerDef: ContainerDef = MongoDBContainer.Def()



  class TrustRegistryFixture extends FutureFixture[Future[AppConfig]]("trustregistry-server") {

    private val appConfigPromise: Promise[AppConfig] = Promise[AppConfig]()
    private val trustRegistryPromise: Promise[TrustRegistryServer] = Promise[TrustRegistryServer]()


    def init(container: MongoDBContainer): Unit = {
        val config = AppConfig(
          mongoUri = container.container.getConnectionString,
          mongoDbName = "test",
          cardano = CardanoConfig.default
        )
        val server = new TrustRegistryServer()
        trustRegistryPromise.success(server)
        server.start(config).onComplete{
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

  override val munitFixtures = List(serverFixture, sttpBackendFixture)

  override def afterContainersStart(container: containerDef.Container): Unit = {
    println(s"afterContainersStart: ${this.suiteDescription}")
    super.afterContainersStart(container)
    serverFixture.init(container.asInstanceOf[MongoDBContainer])
    println("fixture initialized")
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    val p = java.security.Security.addProvider(
      new org.bouncycastle.jce.provider.BouncyCastleProvider()
    );
  }

  test("create and local trust registry") {
    val f = async[Future] {
       val appConfig = await(serverFixture())
       val sttpBackend = sttpBackendFixture()
       val createTrustRegistryRequest = sttp.client3.basicRequest
         .post(uri"http://localhost:${appConfig.port}/trust-registry")
         .body(
            CreateTrustRegistryDTO(
              name = "test",
              network = "local",
            )
         )
      val response = await(sttpBackend.send(createTrustRegistryRequest))
      println(s"response=${response}")
      assert(response.code.isSuccess)
    }
    f
  }

}
