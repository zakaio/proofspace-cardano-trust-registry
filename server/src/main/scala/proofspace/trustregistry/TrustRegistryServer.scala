package proofspace.trustregistry

import com.github.rssh.appcontext.AppContext

import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.*
import cps.*
import cps.monads.{*, given}
import org.apache.pekko.actor.{ActorSystem, Terminated}
import org.apache.pekko.http.scaladsl.Http
import sttp.tapir.*
import sttp.tapir.server.pekkohttp.{PekkoHttpServerInterpreter, PekkoHttpServerOptions}
import proofspace.trustregistry.controllers.RegistryCrudAPI
import proofspace.trustregistry.gateways.TrustRegistryBackend
import proofspace.trustregistry.gateways.local.MongoDBTrustRegistryBackend
import proofspace.trustregistry.services.*
import sttp.tapir.swagger.bundle.SwaggerInterpreter



class TrustRegistryServer {

  private val startPromise = Promise[Boolean]()
  private val finishPromise = Promise[Boolean]()
  private val endPromise = Promise[Terminated]()

  def start(appConfig: AppConfig): Future[Boolean] = async[Future] {
    given AppConfig = appConfig
    val mongoDBService = MongoDBService.create(appConfig).await
    given MongoDBService = mongoDBService
    try
      await(runWithDB)
    finally
      mongoDBService.close().await
  }

  def finish(): Future[Unit] = {
    finishPromise.success(true)
    endPromise.future.map(_ => ())
  }

  def startFuture(): Future[Boolean] = startPromise.future

  def finishFuture(): Future[Boolean] = finishPromise.future

  private def runWithDB(using AppConfig, MongoDBService): Future[Boolean] = async[Future]{
    given AppContext.Cache = AppContext.newCache

    given TrustRegistryBackend = MongoDBTrustRegistryBackend()

    given ActorSystem = ActorSystem()


    val controller = new RegistryCrudAPI();
    val endpoints = controller.endpoints

    val swaggerEndpoints = SwaggerInterpreter().fromEndpoints[Future](endpoints.map(_.endpoint), "ProofSpace TrustRegstryServer", "0.0.1")

    val serverOptions = PekkoHttpServerOptions.customiseInterceptors.serverLog(
      PekkoHttpServerOptions.defaultSlf4jServerLog.logWhenReceived(true)
        .logWhenHandled(true)
        .logLogicExceptions(true)
        .logAllDecodeFailures(true)
    ).notAcceptableInterceptor(None).options


    val routes = PekkoHttpServerInterpreter(serverOptions).toRoute(endpoints ++ swaggerEndpoints)

    val bindingFuture = Http().newServerAt("localhost", 8080).bindFlow(routes)

    bindingFuture.onComplete {
      case Success(binding) =>
        println(s"Server started at ${binding.localAddress}")
        startPromise.success(true)
      case Failure(ex) =>
        println(s"Failed to start server: ${ex.getMessage}")
        startPromise.failure(ex)
    }

    val binding = bindingFuture.await

    val endFuture = finishPromise.future.transformWith {
      case Success(_) =>
        binding.unbind().flatMap(done => summon[ActorSystem].terminate())
      case Failure(ex) =>
        println(s"Failed to stop server: ${ex.getMessage}")
        binding.unbind().flatMap(done => summon[ActorSystem].terminate())
    }
    endPromise.completeWith(endFuture)

    startPromise.future.await
  }


}
