package proofspace.trustregistry

import scala.annotation
import scala.concurrent.{Await, TimeoutException}


object Main  {
  
  
  def main(args: Array[String]): Unit = {

    val cmdLineConfig = CmdLineConfig.parse(args)
    val appConfig = AppConfig.read(cmdLineConfig)
    println("Hello, world!")
    
    val server = new TrustRegistryServer();
    val startFuture = server.start(appConfig)
    val startResult = Await.result(startFuture, scala.concurrent.duration.Duration.Inf)
    println(s"Server started: $startResult")
    var done = false
    val startTime = System.currentTimeMillis()
    while(!done) {
      try 
        Await.result(server.finishFuture(), scala.concurrent.duration.Duration.Inf)
      catch
        case ex:TimeoutException =>
          val now = System.currentTimeMillis()
          println("server running ")
          done = true
    }
  }
  
}
