package proofspace.trustregistry.gateways.cardano.contractTemplates

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cps.*
import cps.monads.FutureAsyncMonad
import com.github.rssh.appcontext.{AppContext, AppContextProvider}
import org.slf4j.LoggerFactory
import org.typelevel.paiges.Document
import proofspace.trustregistry.AppConfig
import proofspace.trustregistry.services.MongoDBService
import reactivemongo.api.{Collection, DB}
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSONDocument, BSONDocumentHandler, Macros}
import reactivemongo.api.indexes.{Index, IndexType}

class ScriptRepository(using AppContextProvider[MongoDBService], AppContextProvider[AppConfig]) {

     import ScriptRepository.{*, given}

     private val logger = LoggerFactory.getLogger(classOf[ScriptRepository])

     def getCollection(db: DB): BSONCollection =
        db.collection("cardano_scripts")

     def saveScript(hash:String, script: Array[Byte], name:Option[String]): Future[Unit] = async[Future] {
       val db = AppContext[MongoDBService].db.await
       val fName = name.getOrElse(hash)
       val doc = BSONDocument("hash" -> hash, "scriptDoubleCBor" -> script, "name" -> fName)
       val inserted = getCollection(db).insert.one(doc).await
     }

     def getScriptByHash(hash: String): Future[Option[Array[Byte]]] = async[Future] {
       val db = AppContext[MongoDBService].db.await
       val collection = getCollection(db)
       val query = BSONDocument("hash" -> hash)
       val result = collection.find(query).one[Entry].await
       result.map(_.scriptDoubleCBor)
     }

     def getHashAndScriptByName(name: String): Future[Option[(String,Array[Byte])]] = async[Future] {
       val db = AppContext[MongoDBService].db.await
       val collection = getCollection(db)
       val query = BSONDocument("name" -> name)
       val result = collection.find(query).one[Entry].await
       result.map(e => (e.hash, e.scriptDoubleCBor))
     }


     def index(): Future[Unit] = async[Future] {
       val db = AppContext[MongoDBService].db.await
       val collection = getCollection(db)
       val indexHash = Index(Seq("hash" -> IndexType.Hashed), unique = true)
       val resultIndexHash = collection.indexesManager.ensure(indexHash).await
       if (!resultIndexHash) {
         logger.info("Failed to create index on hash (probably already exists)")
       }
       val indexName = Index(Seq("name" -> IndexType.Descending), unique = true)
       val resultIndexName = collection.indexesManager.ensure(indexName).await
       if (!resultIndexName) {
         logger.info("Failed to create index on name (probably already exists)")
       }
     }

}

object ScriptRepository {

  case class Entry(hash: String, scriptDoubleCBor: Array[Byte], name:String)

  given BSONDocumentHandler[Entry] = Macros.handler

  given (using AppContextProvider[MongoDBService], AppContextProvider[AppConfig]):AppContextProvider[ScriptRepository] = new AppContextProvider[ScriptRepository] {
    def get: ScriptRepository = new ScriptRepository
  }

}