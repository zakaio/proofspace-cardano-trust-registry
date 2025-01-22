package proofspace.trustregistry.gateways.local

import com.github.rssh.appcontext.{AppContext, AppContextProvider}
import cps.*
import cps.monads.{*, given}
import org.slf4j.LoggerFactory
import proofspace.trustregistry.dto.*
import proofspace.trustregistry.dto.TrustRegistryProposalStatusDTO.Add
import proofspace.trustregistry.gateways.*
import proofspace.trustregistry.services.MongoDBService
import reactivemongo.api.DB
import reactivemongo.api.bson.*
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.indexes.{Index, IndexType}

import java.time.LocalDateTime
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.*

/**
 * Local trust registry backend without blockchain backing.
 * All changes to the trust registry are applied immediatly without voting.
 */
class MongoDBTrustRegistryBackend(using AppContextProvider[MongoDBService], AppContextProvider[BlockChainLocalTrustRegistryAdapter]) extends TrustRegistryBackend {

  import MongoDBTrustRegistryBackend.{*, given}
  private val logger = LoggerFactory.getLogger(classOf[MongoDBTrustRegistryBackend])

  override def name: String = "MongoDBTrustRegistryBackend"

  override def createRegistry(create: CreateTrustRegistryDTO): Future[TrustRegistryDTO] = async[Future]{
    val registryId = AppContext[BlockChainLocalTrustRegistryAdapter].createTrustRegistry(create).await
    val entry = TrustRegistryEntry(registryId, create.name,
      create.network, create.subnetwork, create.targetAdderss,
      Seq.empty)
    val collection = await(retrieveCollection)
    val bsonHandler = summon[BSONHandler[TrustRegistryEntry]]
    val insertResult = await(collection.insert.one(entry))
    if (insertResult.writeErrors.nonEmpty) {
      logger.error(s"Failed to insert trust registry entry: ${insertResult.writeErrors}")
      throw new Exception("Failed to insert trust registry entry")
    }
    // TODO: receive through the blockchain adapter.
    val now = LocalDateTime.now()
    TrustRegistryDTO(registryId, create.name, create.network, create.subnetwork, create.targetAdderss, now)
  }

  override def submitChange(change: TrustRegistryChangeDTO): Future[TrustRegistryChangeDTO] = async[Future]{
    if (change.addedDids.isEmpty && change.removedDids.isEmpty) {
      throw new Exception("Change must contain at least one DID")
    }
    val collection = await(retrieveCollection)
    val optRegistryHeader = await(collection.find(BSONDocument("registryId" -> change.registryId)).one[TrustRegistryHeader])
    val registryHeader = optRegistryHeader.getOrElse(throw new Exception(s"Trust registry ${change.registryId} not found"))
    val changeId = AppContext[BlockChainLocalTrustRegistryAdapter].createTrustRegistryChangeRequest(change).await
    val didChanges = change.addedDids.map(did => TrustRegistryDidEntryInChange(did, Add))
      ++ change.removedDids.map(did => TrustRegistryDidEntryInChange(did, TrustRegistryProposalStatusDTO.Remove))

    val existingChanged = collection.aggregateWith[CountResult](){ framework =>
      import framework.{*,given}
      List(
        Match(BSONDocument("registryId" -> change.registryId)),
        UnwindField("changes"),
        Match(BSONDocument("changes.changeId" -> changeId)),
        Limit(1),
        Count("count")
      )
    }.headOption.await
    if (existingChanged.isDefined) {
      throw new IllegalStateException(s"Change with id ${changeId} in registry ${change.registryId} already exists")
    }
    val entry = TrustRegistryChangeEntry(
      changeId,
      didChanges,
      accepted = false,
      lastDate = LocalDateTime.now()
    )
    val updateResult = await(collection.update.one(
      BSONDocument("registryId" -> change.registryId),
      BSONDocument("$push" -> BSONDocument("changes" -> entry))
    ))
    change.copy(changeId = Some(changeId))
  }

  override def rejectChange(changeId: String): Future[Unit] = ???

  override def approveChange(changeId: String): Future[Unit] = ???

  override def queryEntries(query: TrustRegistryEntryQueryDTO): Future[TrustRegistryDidEntriesDTO] = ???

  override def queryDid(registryId: String, did: String): Future[Option[TrustRegistryDidEntryDTO]] = ???

  private def collectionName = "trust_registries";

  private def retrieveCollection: Future[BSONCollection] = async[Future]{
      val db = await(AppContext[MongoDBService].db)
      db.collection[BSONCollection](collectionName)
  }

  protected def checkIndexes(db: DB): Future[Boolean] = async[Future] {
    val collection = retrieveCollection.await
    val acceptedDidChanges = Index(Seq("accepted.didChanges.did" -> IndexType.Ascending))
    val ensureAcceptedDidChanges = await(collection.indexesManager.ensure(acceptedDidChanges))
    var retval = true
    if (!ensureAcceptedDidChanges) {
      logger.error("Failed to create index for accepted did changes")
      retval = false
    }
    val proposedDidChanges = Index(Seq("proposed.didChanges.did" -> IndexType.Ascending))
    val ensureProposedDidChanges = await(collection.indexesManager.ensure(proposedDidChanges))
    if (!ensureProposedDidChanges) {
      logger.error("Failed to create index for proposed did changes")
      retval = false
    }
    retval
  }

}

//TODO: write test
object MongoDBTrustRegistryBackend {

  case class TrustRegistryHeader(
             registryId: String,
             name: String,
             network: String,
             subnetwork: Option[String],
             targetAddress: Option[String],
                                )

  given BSONDocumentHandler[TrustRegistryHeader] = Macros.handler[TrustRegistryHeader]


  case class TrustRegistryEntry(
                               registryId: String,
                               name: String,
                               network: String,
                               subnetwork: Option[String],
                               targetAddress: Option[String],
                               changes: Seq[TrustRegistryChangeEntry],
                               )

  given BSONDocumentHandler[TrustRegistryEntry] = Macros.handler[TrustRegistryEntry]

  case class TrustRegistryChangeEntry(
                                     changeId: String,
                                     //registry_id
                                     didChanges: Seq[TrustRegistryDidEntryInChange],
                                     accepted: Boolean,
                                     lastDate: LocalDateTime,
                                     )

  given BSONHandler[TrustRegistryChangeEntry] = Macros.handler[TrustRegistryChangeEntry]

  case class TrustRegistryDidEntryInChange(
                                         did: String,
                                         proposal: TrustRegistryProposalStatusDTO,
                                         )


  given BSONHandler[TrustRegistryDidEntryInChange] = Macros.handler[TrustRegistryDidEntryInChange]

  given BSONHandler[TrustRegistryProposalStatusDTO] = new BSONHandler {
    def writeTry(t: TrustRegistryProposalStatusDTO): Try[BSONValue] =
      Success(BSONInteger(TrustRegistryProposalStatusDTO.toInt(t)))
    def readTry(b: BSONValue): Try[TrustRegistryProposalStatusDTO] = b match {
      case BSONInteger(i) => Try(TrustRegistryProposalStatusDTO.fromInt(i))
      case other => Failure(new Exception(s"Invalid BSON type for TrustRegistryProposalStatusDTO (BSONInteger was extepted, we have) "))
    }
  }


  given BSONHandler[TrustRegistryEntryStatusDTO] = new BSONHandler[TrustRegistryEntryStatusDTO]:
    override def readTry(bson: BSONValue): Try[TrustRegistryEntryStatusDTO] = ???

    override def writeTry(t: TrustRegistryEntryStatusDTO): Try[BSONValue] = ???


  given BSONDocumentHandler[TrustRegistryChangeDTO] = Macros.handler

  case class CountResult(count: Int)

  given BSONDocumentHandler[CountResult] = Macros.handler[CountResult]

}