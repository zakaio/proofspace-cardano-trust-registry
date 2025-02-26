package proofspace.trustregistry

import com.github.rssh.appcontext.AppContext

import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.*
import cps.*
import cps.monads.{*, given}
import org.apache.pekko.actor.{ActorSystem, Terminated}
import org.apache.pekko.http.scaladsl.Http
import org.slf4j.LoggerFactory
import sttp.tapir.*
import sttp.tapir.server.pekkohttp.{PekkoHttpServerInterpreter, PekkoHttpServerOptions}
import sttp.tapir.server.interceptor.cors.{CORSConfig, CORSInterceptor}
import sttp.model.Method
import proofspace.trustregistry.controllers.{CardanoScriptAPI, RegistryCrudAPI}
import proofspace.trustregistry.gateways.TrustRegistryBackend
import proofspace.trustregistry.gateways.local.MongoDBTrustRegistryBackend
import proofspace.trustregistry.services.*
import sttp.tapir.swagger.bundle.SwaggerInterpreter



class TrustRegistryServer {

  private val logger = LoggerFactory.getLogger(classOf[TrustRegistryServer])

  private val startPromise = Promise[Boolean]()
  private val finishPromise = Promise[Boolean]()
  private val endPromise = Promise[Terminated]()

  def start(appConfig: AppConfig): Future[Boolean] = async[Future] {
    given AppConfig = appConfig
    val mongoDBService = MongoDBService.create(appConfig).await
    given MongoDBService = mongoDBService
    await(runWithDB)
  }

  def finish(): Future[Unit] = {
    finishPromise.success(true)
    endPromise.future.map(_ => ())
  }

  def startFuture(): Future[Boolean] = startPromise.future

  def finishFuture(): Future[Boolean] = finishPromise.future

  private def runWithDB(using AppConfig, MongoDBService): Future[Boolean] = async[Future]{
    given AppContext.Cache = AppContext.newCache

    given MongoDBTrustRegistryBackend = MongoDBTrustRegistryBackend()

    val indexesCreated = summon[MongoDBTrustRegistryBackend].checkIndexes.await
    
    given ActorSystem = ActorSystem()


    val registryCrudAPI = new RegistryCrudAPI()
    val cardanoScriptApi = new CardanoScriptAPI()
    val endpoints = registryCrudAPI.endpoints ++ cardanoScriptApi.endpoints

    val swaggerEndpoints = SwaggerInterpreter().fromEndpoints[Future](endpoints.map(_.endpoint), "ProofSpace TrustRegstryServer", "0.0.1")

    val corsConfig = CORSConfig.default.allowMatchingOrigins(_ => true)
      .allowMethods(Method.GET, Method.POST, Method.PUT,
        Method.OPTIONS, Method.DELETE, Method.HEAD,
        Method.CONNECT)
      .allowCredentials

    val serverOptions = PekkoHttpServerOptions.customiseInterceptors.serverLog(
      PekkoHttpServerOptions.defaultSlf4jServerLog.logWhenReceived(true)
        .logWhenHandled(true)
        .logLogicExceptions(true)
        .logAllDecodeFailures(true)
    ).corsInterceptor(CORSInterceptor.customOrThrow(corsConfig))
      .notAcceptableInterceptor(None).options


    val routes = PekkoHttpServerInterpreter(serverOptions).toRoute(endpoints ++ swaggerEndpoints)


    val bindingFuture = Http().newServerAt(summon[AppConfig].host, summon[AppConfig].port).bindFlow(routes)

    bindingFuture.onComplete {
      case Success(binding) =>
        logger.info(s"Server started at ${binding.localAddress} ")
        startPromise.success(true)
      case Failure(ex) =>
        logger.error(s"Failed to start server: ${ex.getMessage}", ex)
        startPromise.failure(ex)
    }

    val binding = bindingFuture.await

    val endFuture = finishPromise.future.transformWith { finish =>
      finish match
        case Success(_) =>
          logger.info("Shutting down server")
        case Failure(ex) =>
          logger.info(s"Failed to stop server: ${ex.getMessage}", ex)
      AppContext[MongoDBService].close().flatMap { _ =>
        binding.unbind().flatMap(done => summon[ActorSystem].terminate())
      }
    }
    endPromise.completeWith(endFuture)

    startPromise.future.await
  }


}
