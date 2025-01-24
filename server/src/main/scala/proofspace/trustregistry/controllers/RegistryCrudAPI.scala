package proofspace.trustregistry.controllers

import scala.concurrent.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*
import com.github.rssh.appcontext.*
import proofspace.trustregistry.AppConfig
import proofspace.trustregistry.dto.*
import proofspace.trustregistry.gateways.TrustRegistryBackend

class RegistryCrudAPI(using AppContextProvider[TrustRegistryBackend])  {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val schemaForStatusCode: Schema[StatusCode] = Schema.schemaForInt.map[StatusCode](code => Some(StatusCode.unsafeApply(code)))(_.code)

  lazy val endpoints = List(
    listEndpoint
  )

  val listEndpoint = endpoint.get.in("trust-registry")
    .securityIn(auth.bearer[Option[String]]())
    .securityIn(header[Option[String]]("X-Body-Signature"))
    .in(
      query[Option[String]]("registryId").and(
           query[Option[String]]("name")
        ).and(
           query[Option[Int]]("limit")
        ).and(
           query[Option[Int]]("offset")
        )
    ).mapIn((r,n,l,o) => TrustRegistryQueryDTO(r,n,l,o))(x => (x.registryId,x.name,x.limit,x.offset))
    .errorOut(jsonBody[HttpExceptionDTO])
    .errorOut(statusCode)
    .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
    .description("List trust registry entries")
    .out(jsonBody[TrustRegistriesDTO])
    .serverSecurityLogic { case (optBearer, optSignature) =>
       verifySignature(optBearer, optSignature)
    }.serverLogic { r => query =>
        handleListQuery(query)
    }

    val createEndpoint = endpoint.post.in("trust-registry")
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .in(jsonBody[CreateTrustRegistryDTO])
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Create a trust registry")
      .out(jsonBody[TrustRegistryDTO])
      .serverSecurityLogic { case (optBearer, optSignature) =>
         verifySignature(optBearer, optSignature)
      }.serverLogic { r => create =>
          handleCreateRegistry(create)
      }


    def verifySignature(bearer: Option[String], signature: Option[String]): Future[Either[HttpExceptionDTO,Unit]] = {
      // Verify signature
      Future.successful(Right[HttpExceptionDTO,Unit](()))
    }

    def handleListQuery(query: TrustRegistryQueryDTO): Future[Either[HttpExceptionDTO,TrustRegistriesDTO]] = {

      AppContext[TrustRegistryBackend].listRegistries(query).map(Right(_)).recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

    def handleCreateRegistry(create: CreateTrustRegistryDTO): Future[Either[HttpExceptionDTO,TrustRegistryDTO]] = {
      AppContext[TrustRegistryBackend].createRegistry(create).map(Right(_)).recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

  
}
