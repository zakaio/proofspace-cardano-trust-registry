package proofspace.trustregistry.n2n

import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global
import cps.*
import cps.monads.{*, given}
import com.dimafeng.testcontainers.{ContainerDef, MongoDBContainer}
import com.dimafeng.testcontainers.munit.TestContainerForAll
import munit.FutureFixture
import proofspace.platform.util.ProofspaceDashboardKey
import proofspace.trustregistry.*
import proofspace.trustregistry.dto.*
import proofspace.trustregistry.util.JSoniterDefaultCodecs.{*, given}
import sttp.capabilities.WebSockets
import sttp.capabilities.pekko.PekkoStreams
import sttp.client3.*
import sttp.client3.jsoniter.*
import sttp.client3.pekkohttp.*
import sttp.model.StatusCode


class TestCreateAndEditLocalTrustRegistry extends munit.FunSuite with TestContainerForAll {

  override val containerDef: ContainerDef = MongoDBContainer.Def("mongo:8.0")



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
                requestSigning  = None,
                defaultServiceDid  = Some("testServiceDid")  // TODO: get real service.
              )
            )
          )
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

  test("create add itens to local trust registry") {
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

      // now generate 100 DIDs and add them to the registry
      val dids = (1 to 100).map{ i => s"did:example:${i}" }
      val changeRequest = sttp.client3.basicRequest.post(
          uri"http://localhost:${appConfig.port}/trust-registry/test/change")
        .body(
          TrustRegistryChangeDTO(
            registryId = "local:test",
            changeId = None,
            addedDids = dids,
            removedDids = Seq.empty
          )
        ).response(asJson[TrustRegistryChangeDTO])
      val changeResponse = await(sttpBackend.send(changeRequest))
      println(s"changeResponse=${changeResponse}")
      assert(changeResponse.code.isSuccess)
      val changeId = changeResponse.body match
        case Right(change) => change.changeId.getOrElse(
          throw new RuntimeException("Failed to get changeId from response")
        )
        case Left(error) => throw new Exception(s"Failed to parse response to change request: ${error}")

      val registryId = "local:test"
      // now query the registry for the DIDs
      val entryQuery = TrustRegistryEntryQueryDTO(
        registryId = registryId,
        limit = Some(1000)
      )
      val queryRequest = sttp.client3.basicRequest.get(
          uri"http://localhost:${appConfig.port}/trust-registry/${entryQuery.registryId}/entries?registryId=${entryQuery.registryId}&limit=${entryQuery.limit}"
      ).response(asJson[TrustRegistryDidEntriesDTO])
      val queryResponse = await(sttpBackend.send(queryRequest))
      println(s"queryResponse=${queryResponse}")
      assert(queryResponse.code.isSuccess)
      val entries = queryResponse.body match
        case Right(entries) => entries
        case Left(error) => throw new Exception(s"Failed to parse response: ${error}")
      assert(entries.items.size == 100)
      assert(entries.itemsTotal == 100)

      val item59 = entries.items.find(_.did == "did:example:59")
      assert(item59.isDefined)
      assert(item59.get.proposedChange.isDefined)
      assert(item59.get.proposedChange.get.status == TrustRegistryProposalStatusDTO.Add)

      //naw accept change
      val approveChangeRequest = sttp.client3.basicRequest.post(
          uri"http://localhost:${appConfig.port}/trust-registry/${registryId}/change/${changeId}/approve")
        .response(asJson[Boolean])

      val approveChangeResponse = await(sttpBackend.send(approveChangeRequest))
      //println(s"approveChangeResponse=${approveChangeResponse}")
      assert(approveChangeResponse.code.isSuccess)

      val queryResponse2 = await(sttpBackend.send(queryRequest))
      //println(s"queryResponse2=${queryResponse2}")

      val entries2 = queryResponse2.body match
        case Right(entries) => entries
        case Left(error) => throw new Exception(s"Failed to parse response: ${error}")

      val item59_2 = entries2.items.find(_.did == "did:example:59")
      assert(item59_2.isDefined)
      assert(item59_2.get.acceptedChange.isDefined)
      assert(item59_2.get.acceptedChange.get.status == TrustRegistryProposalStatusDTO.Add)
      assert(item59_2.get.proposedChange.isEmpty)

    }
    f
  }

  test("submit and reject change") {
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

      // now generate 100 DIDs and add them to the registry
      val dids = (1 to 10).map { i => s"did:example:${100+i}" }
      val changeRequest = sttp.client3.basicRequest.post(
          uri"http://localhost:${appConfig.port}/trust-registry/test/change")
        .body(
          TrustRegistryChangeDTO(
            registryId = "local:test",
            changeId = None,
            addedDids = dids,
            removedDids = Seq.empty
          )
        ).response(asJson[TrustRegistryChangeDTO])
      val changeResponse = await(sttpBackend.send(changeRequest))
      println(s"changeResponse=${changeResponse}")
      assert(changeResponse.code.isSuccess)
      val changeId = changeResponse.body match
        case Right(change) => change.changeId.getOrElse(
          throw new RuntimeException("Failed to get changeId from response")
        )
        case Left(error) => throw new Exception(s"Failed to parse response to change request: ${error}")

      val registryId = "local:test"
      // now query the registry for the DIDs
      val entryQuery = TrustRegistryEntryQueryDTO(
        registryId = registryId,
        limit = Some(1000)
      )
      val queryRequest = sttp.client3.basicRequest.get(
        uri"http://localhost:${appConfig.port}/trust-registry/${entryQuery.registryId}/entries?registryId=${entryQuery.registryId}&limit=${entryQuery.limit}"
      ).response(asJson[TrustRegistryDidEntriesDTO])
      val queryResponse = await(sttpBackend.send(queryRequest))
      println(s"queryResponse=${queryResponse}")
      assert(queryResponse.code.isSuccess)
      val entries = queryResponse.body match
        case Right(entries) => entries
        case Left(error) => throw new Exception(s"Failed to parse query response: ${error}")
      assert(entries.items.size > 0)

      val rejectChange = sttp.client3.basicRequest.post(
          uri"http://localhost:${appConfig.port}/trust-registry/${registryId}/change/${changeId}/reject")
        .response(asJson[Boolean])

      val item109_1 = entries.items.find(_.did == "did:example:109")
      assert(item109_1.isDefined)

      val didQuery = TrustRegistryEntryQueryDTO(
        registryId = registryId,
        did = Some("did:example:109")
      )

      val rejectChangeResponse = await(sttpBackend.send(rejectChange))
      println(s"rejectChangeResponse=${rejectChangeResponse}")
      assert(rejectChangeResponse.code.isSuccess)

      val didQueryRequest = sttp.client3.basicRequest.get(
        uri"http://localhost:${appConfig.port}/trust-registry/${didQuery.registryId}/did/${didQuery.did.get}"
      ).response(asJson[Option[TrustRegistryDidEntryDTO]])

      val queryResponse2 = await(sttpBackend.send(didQueryRequest))
      println(s"queryResponse2=${queryResponse2}")
      assert(queryResponse2.code == StatusCode.NotFound)

      val didQuery83 = TrustRegistryEntryQueryDTO(
        registryId = registryId,
        did = Some("did:example:83")
      )
      val queryRequest3 = sttp.client3.basicRequest.get(
        uri"http://localhost:${appConfig.port}/trust-registry/${didQuery83.registryId}/did/${didQuery83.did.get}"
      ).response(asJson[Option[TrustRegistryDidEntryDTO]])
      val queryResponse3 = await(sttpBackend.send(queryRequest3))
      println(s"queryResponse3=${queryResponse3}")  
      

    }
    f
  }

  test("query changes") {
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

      // now generate 10 DIDs and add them to the registry
      val dids = (1 to 10).map { i => s"did:example:${200 + i}" }
      val changeRequest = sttp.client3.basicRequest.post(
          uri"http://localhost:${appConfig.port}/trust-registry/test/change")
        .body(
          TrustRegistryChangeDTO(
            registryId = "local:test",
            changeId = None,
            addedDids = dids,
            removedDids = Seq.empty
          )
        ).response(asJson[TrustRegistryChangeDTO])
      val changeResponse = await(sttpBackend.send(changeRequest))
      println(s"changeResponse=${changeResponse}")
      assert(changeResponse.code.isSuccess)
      val changeId = changeResponse.body match
        case Right(change) => change.changeId.getOrElse(
          throw new RuntimeException("Failed to get changeId from response")
        )
        case Left(error) => throw new Exception(s"Failed to parse response to change request: ${error}")

      val registryId = "local:test"
      //now query the changes
      val changeQuery = TrustRegistryChangeQueryDTO(
        registryId = Some(registryId),
        limit = Some(1000)
      )
      val queryRequest = sttp.client3.basicRequest.get(
        uri"http://localhost:${appConfig.port}/trust-registry/${changeQuery.registryId.get}/changes?registryId=${changeQuery.registryId.get}&limit=${changeQuery.limit}"
      ).response(asJson[TrustRegistryChangesDTO])

      val queryResponse = await(sttpBackend.send(queryRequest))
      println(s"changes queryResponse=${queryResponse}")
      assert(queryResponse.code.isSuccess)

      val changes = queryResponse.body match
        case Right(changes) => changes
        case Left(error) => throw new Exception(s"Failed to parse query response: ${error}")


      assert(changes.items.size > 0)
      assert(changes.itemsTotal > 0)

    }
    f
  }



}
