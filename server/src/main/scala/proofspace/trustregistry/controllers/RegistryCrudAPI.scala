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
import sttp.tapir.server.ServerEndpoint

class RegistryCrudAPI(using AppContextProvider[TrustRegistryBackend]) extends BaseTapirController {

  import scala.concurrent.ExecutionContext.Implicits.global


  lazy val endpoints: List[ServerEndpoint[Any,Future]] = List(
    listEndpoint,
    createEndpoint,
    removeEndpoint,
    submitChangeEndpoint,
    approveChangeEndpoint,
    rejectChangeEndpoint,
    queryEntriesEndpoint,
    checkDidEndpoint
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

    val removeEndpoint = endpoint.delete.in("trust-registry" / path[String]("registryId"))
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Remove a trust registry")
      .out(jsonBody[Boolean])
      .serverSecurityLogic { case (optBearer, optSignature) =>
         verifySignature(optBearer, optSignature)
      }.serverLogic { r => registryId =>
          handleRemoveRegistry(registryId)
      }

    def submitChangeEndpoint = endpoint.post.in("trust-registry" / path[String]("registryId") / "change")
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .in(jsonBody[TrustRegistryChangeDTO])
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Submit a change request to a trust registry")
      .out(jsonBody[TrustRegistryChangeDTO])
      .serverSecurityLogic { case (optBearer, optSignature) =>
         verifySignature(optBearer, optSignature)
      }.serverLogic { r => (registryId, change) =>
          handleSubmitChange(change)
      }

    def approveChangeEndpoint = endpoint.post.in("trust-registry" / path[String]("registryId") / "change" / path[String]("changeId") / "approve")
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Approve a change request in a trust registry")
      .out(jsonBody[Boolean])
      .serverSecurityLogic { case (optBearer, optSignature) =>
         verifySignature(optBearer, optSignature)
      }.serverLogic { r => (registryId, changeId) =>
          handleApproveChange(registryId, changeId)
      }

    def rejectChangeEndpoint = endpoint.post.in("trust-registry" / path[String]("registryId") / "change" / path[String]("changeId") / "reject")
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Reject a change request in a trust registry")
      .out(jsonBody[Boolean])
      .serverSecurityLogic { case (optBearer, optSignature) =>
         verifySignature(optBearer, optSignature)
      }.serverLogic { r => (registryId, changeId) =>
          handleRejectChange(registryId, changeId)
      }

    def queryEntriesEndpoint = endpoint.get
      .in("trust-registry" / path[String]("registryId") / "entries")
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .in(
        query[Option[String]]("did").and(
             query[Option[Int]]("limit")
          ).and(
             query[Option[Int]]("offset")
          ).and(
            query[Option[String]]("orderBy")
          ).and(
            query[Option[String]]("orderByDirection")
        )
      ).mapIn(
        (registryId, did, limit, offset, orderBy, direction) =>
           TrustRegistryEntryQueryDTO(registryId,limit,offset, orderBy,direction, did))
        (x => (x.registryId, x.did, x.limit,x.offset, x.orderBy, x.orderByDirection))
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Query trust registry entries")
      .out(jsonBody[TrustRegistryDidEntriesDTO])
      .serverSecurityLogic { case (optBearer, optSignature) =>
         verifySignature(optBearer, optSignature)
      }.serverLogic { r => query =>
          handleQueryEntries(query)
      }

    val checkDidEndpoint = endpoint.get
      .in("trust-registry" / path[String]("registryId") / "did" / path[String]("did"))
      .securityIn(auth.bearer[Option[String]]())
      .securityIn(header[Option[String]]("X-Body-Signature"))
      .errorOut(jsonBody[HttpExceptionDTO])
      .errorOut(statusCode)
      .mapErrorOut(Mapping.from[(HttpExceptionDTO,StatusCode),HttpExceptionDTO](_._1)(ex => (ex,StatusCode(ex.statusCode))))
      .description("Check if a DID is in a trust registry")
      .out(jsonBody[TrustRegistryDidEntryDTO])
      .serverSecurityLogic { case (optBearer, optSignature) =>
         verifySignature(optBearer, optSignature)
      }.serverLogic { r => (registryId, did) =>
          handleQueryDid(registryId, did)
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

    def handleRemoveRegistry(registryId: String): Future[Either[HttpExceptionDTO,Boolean]] = {
      AppContext[TrustRegistryBackend].removeRegistry(registryId).map(Right(_)).recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

    def handleSubmitChange(change: TrustRegistryChangeDTO): Future[Either[HttpExceptionDTO,TrustRegistryChangeDTO]] = {
      AppContext[TrustRegistryBackend].submitChange(change).map(Right(_)).recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

    def handleApproveChange(registryId: String, changeId: String): Future[Either[HttpExceptionDTO,Boolean]] = {
      AppContext[TrustRegistryBackend].approveChange(registryId, changeId).map(Right(_)).recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

    def handleRejectChange(registryId: String, changeId: String): Future[Either[HttpExceptionDTO,Boolean]] = {
      AppContext[TrustRegistryBackend].rejectChange(registryId, changeId).map(Right(_)).recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

    def handleQueryEntries(query: TrustRegistryEntryQueryDTO): Future[Either[HttpExceptionDTO,TrustRegistryDidEntriesDTO]] = {
      AppContext[TrustRegistryBackend].queryEntries(query).map(Right(_)).recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

    def handleQueryDid(registryId: String, did: String): Future[Either[HttpExceptionDTO,TrustRegistryDidEntryDTO]] = {
      AppContext[TrustRegistryBackend].queryDid(registryId, did).map {
        case Some(entry) => Right(entry)
        case None => Left(HttpExceptionDTO(StatusCode.NotFound.code, s"Entry with DID $did not found"))
      }.recover {
        case ex:HttpException => Left(ex.toDTO)
      }
    }

  
}
