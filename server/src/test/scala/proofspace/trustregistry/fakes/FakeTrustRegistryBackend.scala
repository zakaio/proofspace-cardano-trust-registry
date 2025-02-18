package proofspace.trustregistry.fakes

import com.github.rssh.appcontext.{AppContext, AppContextProvider}
import org.slf4j.LoggerFactory
import proofspace.trustregistry.AppConfig
import proofspace.trustregistry.dto.TrustRegistryEntryStatusDTO.Active
import proofspace.trustregistry.dto.TrustRegistryProposalStatusDTO.Add
import proofspace.trustregistry.dto.*
import scala.collection.mutable.Queue
import scala.collection.mutable.Map as MutableMap
import scala.collection.concurrent.TrieMap
import proofspace.trustregistry.gateways.TrustRegistryBackend

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.Future


class FakeTrustRegistryBackend(using AppContextProvider[AppConfig]) extends TrustRegistryBackend {
  
  val registries: TrieMap[String, FakeTrustRegistryBackend.InMemoryTrustRegistry] = TrieMap.empty

  override def name: String = "FakeTrustRegistryBackend"

  override def listRegistries(query: TrustRegistryQueryDTO, serviceDid: String, proofspaceNetwork: String): Future[TrustRegistriesDTO] = {
    val retval = registries.flatMap{
      case (name, registry) =>
        if (query.registryId.isDefined && query.registryId.get != name) {
          None
        } else if (query.name.isDefined && query.name.get != name) {
          None
        } else {
          val now = LocalDateTime.now()
          Some(TrustRegistryDTO(name, name, "fake", serviceDid, proofspaceNetwork, None, None, now, None))
        }
    }
    Future.successful(TrustRegistriesDTO(retval.toSeq, retval.size))
  }

  override def createRegistry(create: CreateTrustRegistryDTO): Future[TrustRegistryDTO] = {
    val (serviceDid, proofspaceNetwork) = AppContext[AppConfig].retrieveProofspaceServiceDidAndNetwork(
      create.proofspaceServiceDid, create.proofspaceNetwork
    )
    registries.get(create.name) match
      case Some(_) => throw new Exception(s"Registry with name ${create.name} already exists")
      case None =>
        val registry = new FakeTrustRegistryBackend.InMemoryTrustRegistry
        registries.put(name, registry)
        val now = LocalDateTime.now()
        Future.successful(TrustRegistryDTO(create.name, create.name, create.network, serviceDid, proofspaceNetwork, None, None, now, None))
  }
  
  override def removeRegistry(registryId: String, serviceDid: String, proofspaceNetwork: String): Future[Boolean] = {
    registries.remove(registryId) match
      case Some(_) => Future.successful(true)
      case None => Future.successful(false)
  }

  override def queryDid(registryId: String, did: String): Future[Option[TrustRegistryDidEntryDTO]] = {
    registries.get(registryId) match {
      case Some(registry) =>
        val entry = registry.entries.get(did)
        entry match {
          case Some(entry) =>
            val didChange = entry.lastChange.changeId.map{ changeId =>
              TrustRegistryDidChangeDTO(changeId, Add, entry.lastChangeDate.atOffset(ZoneOffset.UTC))
            }
            val dto = TrustRegistryDidEntryDTO(entry.did, Active , didChange, None)
            Future.successful(Some(dto))
          case None => Future.successful(None)
        }
      case None => throw new Exception(s"Registry with id ${registryId} not found")
    }
  }

  override def queryEntries(query: TrustRegistryEntryQueryDTO): Future[TrustRegistryDidEntriesDTO] = {
    registries.get(query.registryId) match
      case Some(registry) =>
        val (entries, total) = registry.queryEntries(query)
        val items = entries.map{ entry =>
          val didChange = entry.lastChange.changeId.map{ changeId =>
            TrustRegistryDidChangeDTO(changeId, Add, entry.lastChangeDate.atOffset(ZoneOffset.UTC))
          }
          TrustRegistryDidEntryDTO(entry.did, Active, didChange, None)
        }
        Future.successful(TrustRegistryDidEntriesDTO(items,total))
      case None =>
        throw new Exception(s"Registry with id ${query.registryId} not found")
  }


  override def submitChange(change: TrustRegistryChangeDTO, serviceDid: String, proofspaceNetwork: String): Future[TrustRegistryChangeDTO] = {
    registries.get(change.registryId) match {
      case Some(registry) =>
        val entry = registry.applyEntry(change)
        Future.successful(change)
      case None => throw new IllegalArgumentException(s"Registry with id ${change.registryId} not found")
    }
  }
  
  override def rejectChange(registryId: String, changeId: String, serviceDid: String, proofspaceNetwork: String): Future[Boolean] = {
    throw new NotImplementedError("Not implemented")
  }
  
  override def approveChange(registryId: String, changeId: String, serviceDid: String, proofspaceNetwork: String): Future[Boolean] = {
    Future successful(true)
  }

  override def queryChanges(query: TrustRegistryChangeQueryDTO): Future[TrustRegistryChangesDTO] = {
    registries.get(query.registryId.get) match {
      case Some(registry) =>
        val changes = registry.queryChanges(query)
        val items = changes
        Future.successful(TrustRegistryChangesDTO(items, changes.size))
      case None =>
        throw new Exception(s"Registry with id ${query.registryId} not found")
    }
  }
  
}



object FakeTrustRegistryBackend {

  val logger = LoggerFactory.getLogger(classOf[FakeTrustRegistryBackend.type])

  case class Entry(
    did: String,
    status: String,
    lastChange: TrustRegistryChangeDTO,
    lastChangeDate: LocalDateTime
                  )

  
  class InMemoryTrustRegistry {
    val entries: TrieMap[String, Entry] = TrieMap.empty
    // TODO: model voting for entries
    val changes: TrieMap[String, TrustRegistryChangeDTO] = TrieMap.empty

    def applyEntry(change: TrustRegistryChangeDTO): Future[Unit] = {
      val nChangeId = change.changeId.getOrElse(UUID.randomUUID().toString)
      changes.put(nChangeId, change)
      for(did <- change.addedDids) {
        entries.get(did) match
          case Some(entry) =>
            // error (already added did),  but
            // for now we just ignore it
            logger.info(s"Did $did already exists in registry, ignoring")
          case None =>
            val now = LocalDateTime.now()
            val entry = Entry(did, "active", change, now)
            entries.put(did, entry)
      }
      for(did <- change.removedDids) {
        entries.get(did) match
          case Some(entry) =>
            val now = LocalDateTime.now()
            val newEntry = entry.copy(status = "removed", lastChange = change, lastChangeDate = now)
            entries.put(did, newEntry)
          case None =>
            // error (did not found),  but
            // for now we just ignore it
            logger.info(s"Did $did not found in registry, ignoring")
      }
      val r: Unit = ()
      Future.successful(r)
    }

    def queryDid(did: String): Option[Entry] = {
      entries.get(did)
    }

    def queryEntries(query: TrustRegistryEntryQueryDTO): ( Seq[Entry], Int) = {
      val fun = (entry: Entry) => {
        query.did match
          case Some(did) => entry.did == did
          case None => true
      }
      val filtered = entries.values.filter(fun).toSeq
      val sorted0 = query.orderBy.getOrElse("lastChangeDate") match
            case "did" => filtered.toSeq.sortBy(_.did)
            case "status" => filtered.toSeq.sortBy(_.status)
            case "lastChangeDate" => filtered.toSeq.sortBy(_.lastChangeDate)
            case order => throw new Exception(s"Unknown order by field $order")
      val sorted = if (query.orderByDirection.contains("desc")) {
          sorted0.reverse
      } else {
          sorted0
      }
      val total = sorted.size
      val offset = query.offset.getOrElse(0)
      val withOffset = sorted.drop(offset)
      val limit = query.limit.getOrElse(100)
      val withLimit = withOffset.take(limit)
      (withLimit, total)
    }

    def queryChanges(query: TrustRegistryChangeQueryDTO): Seq[TrustRegistryChangeDTO] = {
      val fun = (change: TrustRegistryChangeDTO) => {
        query.changeId match
          case Some(changeId) => change.changeId.contains(changeId)
          case None => true
      }
      changes.values.filter(fun).toSeq
    }

  }



}
