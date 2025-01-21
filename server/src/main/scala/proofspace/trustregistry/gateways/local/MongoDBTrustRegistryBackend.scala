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

  import MongoDBTrustRegistryBackend.*
  private val logger = LoggerFactory.getLogger(classOf[MongoDBTrustRegistryBackend])

  override def name: String = "MongoDBTrustRegistryBackend"

  override def createRegistry(create: CreateTrustRegistryDTO): Future[TrustRegistryDTO] = async[Future]{
    val registryId = AppContext[BlockChainLocalTrustRegistryAdapter].createTrustRegistry(create).await

    ???
  }

  override def submitChange(change: TrustRegistryChangeDTO): Future[TrustRegistryChangeDTO] = ???

  override def rejectChange(changeId: String): Future[Unit] = ???

  override def approveChange(changeId: String): Future[Unit] = ???

  override def queryEntries(query: TrustRegistryEntryQueryDTO): Future[TrustRegistryEntriesDTO] = ???

  override def queryDid(registryId: String, did: String): Future[Option[TrustRegistryEntryDTO]] = ???

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

  case class TrustRegistryEntry(
                               id: String,
                               name: String,
                               acceptedChanges: Seq[TrustRegistryChangeEntry],
                               proposedChanges: Seq[TrustRegistryChangeEntry],
                               )

  given BSONHandler[TrustRegistryEntry] = Macros.handler[TrustRegistryEntry]

  case class TrustRegistryChangeEntry(
                                     id: String,
                                     //registry_id
                                     didChanges: Seq[TrustRegistryDidEntryInChange],
                                     status: TrustRegistryEntryStatusDTO,
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


  given BSONHandler[TrustRegistryChangeDTO] = Macros.handler



}