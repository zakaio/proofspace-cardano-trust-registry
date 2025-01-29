package proofspace.trustregistry.gateways.local

import com.github.rssh.appcontext.{AppContext, AppContextProvider}
import cps.*
import cps.monads.{*, given}
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import proofspace.trustregistry.AppConfig
import proofspace.trustregistry.dto.*
import proofspace.trustregistry.dto.TrustRegistryProposalStatusDTO.Add
import proofspace.trustregistry.gateways.*
import proofspace.trustregistry.services.{BlockchainAdapterService, MongoDBService}
import reactivemongo.api.DB
import reactivemongo.api.bson.{BSONValue, *}
import reactivemongo.api.bson.BSONDocument.pretty
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.play.json.compat.{*, given}
import reactivemongo.api.indexes.{Index, IndexType}

import java.time.LocalDateTime
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.*

/**
 * Local trust registry backend.
 * Blockchain backing is specifiied by BlockChainAdapterService,  which is used to create blockchain adapter
 * for specific network.
 */
class MongoDBTrustRegistryBackend(using AppContextProvider[MongoDBService], AppContextProvider[AppConfig], AppContextProvider[BlockchainAdapterService]) extends TrustRegistryBackend {

  import MongoDBTrustRegistryBackend.{*, given}
  private val logger = LoggerFactory.getLogger(classOf[MongoDBTrustRegistryBackend])

  override def name: String = "MongoDBTrustRegistryBackend"

  override def createRegistry(create: CreateTrustRegistryDTO): Future[TrustRegistryDTO] = async[Future]{
    logger.debug(s"Creating trust registry ${create.name}")
    val blockchainAdapter = AppContext[BlockchainAdapterService].createBlockchainAdapter(create.network, create.subnetwork)
    val registryId = blockchainAdapter.createTrustRegistry(create).await
    val entry = TrustRegistryEntry(registryId, create.name,
      create.network, create.subnetwork, create.targetAdderss,
      Seq.empty)
    val collection = await(retrieveCollection)
    val insertResult = await(collection.insert.one(entry))
    if (insertResult.writeErrors.nonEmpty) {
      logger.error(s"Failed to insert trust registry entry: ${insertResult.writeErrors}")
      throw new Exception("Failed to insert trust registry entry")
    }
    // TODO: receive through the blockchain adapter.
    val now = LocalDateTime.now()
    TrustRegistryDTO(registryId, create.name, create.network, create.subnetwork, create.targetAdderss, now)
  }

  override def listRegistries(query: TrustRegistryQueryDTO): Future[TrustRegistriesDTO] = async[Future] {
    val collection = await(retrieveCollection)
    val mongoQuery0 = BSONDocument()
    val mongoQuery1 = query.registryId match
      case Some(registryId) => mongoQuery0 ++ BSONDocument("registryId" -> registryId)
      case None => mongoQuery0
    val mongoQuery = mongoQuery1
    val limit = query.limit.getOrElse(1000)
    val offset = query.offset.getOrElse(0)
    val registries = collection.find(mongoQuery).skip(offset).cursor[TrustRegistryHeader]().collect[List](limit).await
    TrustRegistriesDTO(registries.map{ header =>
      TrustRegistryDTO(header.registryId, header.name, header.network, header.subnetwork, header.targetAddress, LocalDateTime.now())
    }, registries.size)
  }

  override def removeRegistry(registryId: String): Future[Boolean] = async[Future]{
    val collection = retrieveCollection.await
    val deleteResult = collection.delete.one(BSONDocument("registryId" -> registryId)).await
    deleteResult.n == 1
  }

  override def submitChange(change: TrustRegistryChangeDTO): Future[TrustRegistryChangeDTO] = async[Future]{
    if (change.addedDids.isEmpty && change.removedDids.isEmpty) {
      throw new Exception("Change must contain at least one DID")
    }
    val collection = await(retrieveCollection)
    val optRegistryHeader = await(collection.find(BSONDocument("registryId" -> change.registryId)).one[TrustRegistryHeader])
    val registryHeader = optRegistryHeader.getOrElse(throw new Exception(s"Trust registry ${change.registryId} not found"))
    val blockchainAdapter = AppContext[BlockchainAdapterService].createBlockchainAdapter(registryHeader.network, registryHeader.subnetwork)
    val changeId = blockchainAdapter.createTrustRegistryChangeRequest(change).await
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

  override def rejectChange(registryId: String, changeId: String): Future[Boolean] = async[Future]{
    // write mongodb query wich remove change with changeId
    val collection = await(retrieveCollection)
    val searchQuery = BSONDocument(
      "registryId" -> registryId,
      "changes" -> BSONDocument(
        "$elemMatch" -> BSONDocument("changeId" -> changeId)
      )
    )
    val updateQuery = BSONDocument(
      "changes" -> BSONDocument(
        "$pull" -> BSONDocument("changeId" -> changeId)
      )
    )
    val updateResult = collection.findAndUpdate[BSONDocument,BSONDocument](searchQuery, updateQuery, fields=Some(TrustRegistryHeader.fields)).await
    updateResult.result[TrustRegistryHeader].isDefined
  }

  override def approveChange(registryId: String, changeId: String): Future[Boolean] = async[Future] {
    val collection = await(retrieveCollection)
    val searchQuery = BSONDocument(
      "registryId" -> registryId,
      "changes" -> BSONDocument(
        "$elemMatch" -> BSONDocument("changeId" -> changeId)
      )
    )
    val updateQuery = BSONDocument(
      "$set" -> BSONDocument("changes.$.accepted" -> true)
    )
    //val arrayFilter = BSONDocument("element" -> BSONDocument("$eq" -> BSONArray(List("$changeId",changeId))))
    println(s"update seqrch Query: ${pretty(searchQuery)}")
    //val updateResult = collection.findAndUpdate(searchQuery, updateQuery,
    //   arrayFilters=Seq(arrayFilter), fetchNewObject = true).await
    val updateModifier = collection.updateModifier(
      BSONDocument("$set" -> BSONDocument("changes.$.accepted" -> true)),
      fetchNewObject = true,

    )
    //val updateResult = collection.findAndModify(searchQuery, updateModifier,   fetchNewObject = true).await
    val updateResult = collection.update(ordered = false).one(searchQuery, updateQuery).await
    //val cl1 = collection.runCommand(BSONDocument("findAndModify" -> collectionName, "query" -> searchQuery, "update" -> updateQuery)).await

    //val updateResult = collection.runCommand(

    //)

    println(s"updateResult.value=${updateResult}")

    val entries = collection.find(searchQuery).cursor[BSONDocument]().collect[List]().await
    println(s"after update: entries=${entries.map(x => pretty(x))}")
    // now get value from updateResult and check if it is defined


    updateResult.n == 1
  }

  override def queryEntries(query: TrustRegistryEntryQueryDTO): Future[TrustRegistryDidEntriesDTO] = async[Future]{
    val matchQuery0 = BSONDocument("registryId" -> query.registryId)
    val matchQuery1 = query.did match
      case Some(did) => matchQuery0 ++ BSONDocument("changes.didChanges.did" -> did)
      case None => matchQuery0
    val collection = retrieveCollection.await

    val matchUnwindStages = List(
      BSONDocument("$match" -> matchQuery1),
      BSONDocument("$unwind" -> "$changes"),
      BSONDocument("$unwind" -> "$changes.didChanges"),
    )

    val didOptStages = (
      query.did match
        case Some(did) =>
          List(
            BSONDocument("$match" -> BSONDocument("changes.didChanges.did" -> did))
          )
        case None => List.empty
      )

    val splitAcceptProposedChanges =  List(
      BSONDocument("$addFields" -> BSONDocument(
        "accepted_changes" -> BSONDocument("$cond" -> BSONDocument(
          "if" -> BSONDocument("$eq" -> BSONArray("$changes.accepted", true)),
          "then" -> "$changes",
          "else" -> BSONNull
        )),
        "proposed_changes" -> BSONDocument("$cond" -> BSONDocument(
          "if" -> BSONDocument("$eq" -> BSONArray("$changes.accepted", false)),
          "then" -> "$changes",
          "else" -> BSONNull
        )),
        "did" -> "$changes.didChanges.did",
      )),
      BSONDocument("$unset" -> "changes"),
    )
    val groupByStep = BSONDocument("$group" -> BSONDocument(
      "_id" -> "$did",
      "accepted_changes" -> BSONDocument("$push" -> BSONDocument(
        "changeId" -> "$accepted_changes.changeId",
        "status" -> "$accepted_changes.didChanges.proposal",
        "changeDate" -> "$accepted_changes.lastDate"
      )),
      "proposed_changes" -> BSONDocument("$push" -> BSONDocument(
        "changeId" -> "$proposed_changes.changeId",
        "status" -> "$proposed_changes.didChanges.proposal",
        "changeDate" -> "$proposed_changes.lastDate"
      ))
    ))
    val selectLastChangesStep = BSONDocument("$addFields" -> BSONDocument(
      "did" -> "$_id",
      "lastAcceptedChange" -> BSONDocument("$arrayElemAt" -> BSONArray(
        BSONDocument(
          "$slice" -> BSONArray(BSONDocument(
            "$sortArray" -> BSONDocument(
              "input" -> "$accepted_changes",
              "sortBy" -> BSONDocument("changeDate" -> 1)
            )
          ), -1)
        ), -1
      )),
      "lastProposedChange" -> BSONDocument("$arrayElemAt" -> BSONArray(
        BSONDocument(
          "$slice" -> BSONArray(BSONDocument(
            "$sortArray" -> BSONDocument(
              "input" -> "$proposed_changes",
              "sortBy" -> BSONDocument("changeDate" -> 1)
            )
          ), -1)
        ), -1))
    ))

    def opZeroField(name:String): BSONDocument =
      BSONDocument("$eq" -> BSONArray(
        BSONDocument("$size" -> BSONDocument("$objectToArray" -> s"$$$name")),
        0,
      ))

    def removeIfZero(name:String): BSONDocument =
      BSONDocument(
        "$cond" -> BSONDocument(
          "if" -> opZeroField(name),
          "then" -> "$$REMOVE",
          "else" -> s"$$$name"
        )
      )

    val finalProjStep = BSONDocument("$project" -> BSONDocument(
      "_id" -> 0,
      "did" -> 1,
      "acceptedChange" -> removeIfZero("lastAcceptedChange"),
      "proposedChange" -> removeIfZero("lastProposedChange"),
      "accZero" -> opZeroField("lastAcceptedChange"),
      "propZero" -> opZeroField("lastProposedChange"),
      "status" -> BSONDocument("$cond" -> BSONDocument(
        "if" -> opZeroField("lastAcceptedChange"),
        "then" -> BSONDocument(
          "$cond" -> BSONDocument(
            "if" -> BSONDocument("$eq" -> BSONArray("$lastProposedChange.status", 1)),
            "then" -> "Candidate",
            "else" -> "WithdrawnCandidate"
          )
        ),
        "else" -> BSONDocument(
          "$cond" -> BSONDocument(
            "if" -> BSONDocument("$eq" -> BSONArray("$lastAcceptedChange.status", 1)),
            "then" -> BSONDocument(
              "$cond" -> BSONDocument(
                "if" -> BSONDocument("$eq" -> BSONArray("$lastProposedChange.status", -1)),
                "then" -> "WithdrawnCandidate",
                "else" -> "Active"
              )),
            "else" -> BSONDocument(
              "$cond" -> BSONDocument(
                "if" -> BSONDocument("$eq" -> BSONArray("$lastProposedChange.status", 1)),
                "then" -> "Candidate",
                "else" -> "Withdrawn"
              )
            )
          )
        )
      ))
    ))

    val aggregateCommonQuery = matchUnwindStages ++ didOptStages ++ splitAcceptProposedChanges
      ++ List(groupByStep,  selectLastChangesStep , finalProjStep )

    val countQuery = aggregateCommonQuery ++ List(
      BSONDocument("$count" -> "count")
    )

    var dataQuery = aggregateCommonQuery ++
      (query.orderBy match
        case Some(fieldName) =>
          List(BSONDocument("$sort" -> fieldName))
        case None => List.empty
      ) ++
      List(
        BSONDocument("$skip" -> query.offset.getOrElse(0)),
        BSONDocument("$limit" -> query.limit.getOrElse(1000))
      )


    //val entries1 = collection.aggregateWith[BSONDocument]() { framework =>
    //  (matchUnwindStages ++ didOptStages ++ splitAcceptProposedChanges
    //     ++ List(groupByStep, selectLastChangesStep , finalProjStep ) ).map(framework.PipelineOperator(_))
    //}.collect[Seq]().await
    //println(s"entries1=${entries1.map(e => pretty(e))}")


    val entries = collection.aggregateWith[TrustRegistryDidEntryDTO](){ framework =>
      dataQuery.map(framework.PipelineOperator(_))
    }.collect[Seq]().await


    val countEntries = collection.aggregateWith[CountResult](){ framework =>
      countQuery.map(framework.PipelineOperator(_))
    }.headOption.await

    val total = countEntries.map(_.count).getOrElse(0)

    TrustRegistryDidEntriesDTO(entries, total)
  }

  override def queryDid(registryId: String, did: String): Future[Option[TrustRegistryDidEntryDTO]] = {
    val query = TrustRegistryEntryQueryDTO(registryId, did = Some(did))
    queryEntries(query).map{ entries =>
      entries.items.headOption
    }
  }

  private def collectionName = "trust_registries";

  private def retrieveCollection: Future[BSONCollection] = async[Future]{
      val db = await(AppContext[MongoDBService].db)
      db.collection[BSONCollection](collectionName)
  }

  def checkIndexes: Future[Boolean] = async[Future] {
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

  object TrustRegistryHeader {
    given BSONDocumentHandler[TrustRegistryHeader] = Macros.handler[TrustRegistryHeader]
    val fields = BSONDocument(
      "registryId" -> BSONString("registryId"),
      "name" -> BSONString("name"),
      "network" -> BSONString("network"),
      "subnetwork" -> BSONString("subnetwork"),
      "targetAddress" -> BSONString("targetAddress"),
    )
  }



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
    override def readTry(bson: BSONValue): Try[TrustRegistryEntryStatusDTO] = {
      bson match {
        case BSONString("Active") => Success(TrustRegistryEntryStatusDTO.Active)
        case BSONString("Candidate") => Success(TrustRegistryEntryStatusDTO.Candidate)
        case BSONString("Withdrawn") => Success(TrustRegistryEntryStatusDTO.Withdrawn)
        case BSONString("WithdrawnCandidate") => Success(TrustRegistryEntryStatusDTO.WithdrawnCandidate)
        case _ => Failure(new Exception(s"Invalid BSON type for TrustRegistryEntryStatusDTO (BSONString was extepted, we have) "))
      }
    }

    override def writeTry(t: TrustRegistryEntryStatusDTO): Try[BSONValue] = {
      t match {
        case TrustRegistryEntryStatusDTO.Active => Success(BSONString("Active"))
        case TrustRegistryEntryStatusDTO.Candidate => Success(BSONString("Candidate"))
        case TrustRegistryEntryStatusDTO.Withdrawn => Success(BSONString("Withdrawn"))
        case TrustRegistryEntryStatusDTO.WithdrawnCandidate => Success(BSONString("WithdrawnCandidate"))
      }
    }


  given BSONDocumentHandler[TrustRegistryChangeDTO] = Macros.handler

  case class CountResult(count: Int)

  given BSONDocumentHandler[CountResult] = Macros.handler[CountResult]

  given BSONDocumentHandler[TrustRegistryDidEntryDTO] = Macros.handler[TrustRegistryDidEntryDTO]

  given BSONDocumentHandler[TrustRegistryDidChangeDTO] = Macros.handler[TrustRegistryDidChangeDTO]

}