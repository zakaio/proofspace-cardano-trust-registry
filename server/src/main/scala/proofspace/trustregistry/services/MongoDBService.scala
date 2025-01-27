package proofspace.trustregistry.services

import com.github.rssh.appcontext.*
import org.slf4j.LoggerFactory
import proofspace.trustregistry.AppConfig
import reactivemongo.api.{AsyncDriver, DB, MongoConnection}

import scala.concurrent.*
import scala.concurrent.duration.*


class MongoDBService(private val _connection: MongoConnection, dbName: String) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def db: Future[DB] = {
    //TODO: recoonect if connection was cloased and driver not reconnected automatically
    //  need to check if driver is reconnected automaticall
    _connection.database(dbName)
  }

  def close(): Future[Unit] = {
    //implicit val timeout = AkkaTimeout(5.seconds)
    _connection.close()(using 1.minute).map(_ => ())
  }

}

object MongoDBService {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = LoggerFactory.getLogger(classOf[MongoDBService])

  def create( appConfig: AppConfig): Future[MongoDBService] = {
    val mongoUri = appConfig.mongoUri
    val mongoDbName = appConfig.mongoDbName
    val driver = AsyncDriver()
    for{
      parsedMongoUri <- MongoConnection.fromString(mongoUri)
      connection <- driver.connect(parsedMongoUri)
      //db <- connection.database(mongoDbName)
    } yield {
      logger.info(s"Connected to MongoDB: $mongoUri db: $mongoDbName")
      new MongoDBService(connection, mongoDbName)
    }
  }

}
