package proofspace.trustregistry

import org.slf4j.LoggerFactory

import scala.annotation
import scala.concurrent.{Await, TimeoutException}


object Main  {

  val logger = LoggerFactory.getLogger(classOf[Main.type ])

  def main(args: Array[String]): Unit = {

    val cmdLineConfig = CmdLineConfig.parse(args)
    val appConfig = AppConfig.read(cmdLineConfig)
    logger.info(s"Trustregistry server: Hello, world!, cfg=${appConfig}")

    val server = new TrustRegistryServer();
    val startFuture = server.start(appConfig)
    val startResult = Await.result(startFuture, scala.concurrent.duration.Duration.Inf)
    logger.info(s"Server started: $startResult")
    var done = false
    val startTime = System.currentTimeMillis()
    while(!done) {
      try
        done = Await.result(server.finishFuture(), scala.concurrent.duration.Duration.Inf)
      catch
        case ex:TimeoutException =>
          val now = System.currentTimeMillis()
          logger.info(s"server running ${(now - startTime)/1000} seconds")
    }
    logger.info("server finished")
  }

}
