package ensime.autoresponder

import akka.actor.ActorSystem
import akka.stream.ActorAttributes.SupervisionStrategy
import akka.stream.ActorMaterializer
import akka.stream.Supervision
import com.typesafe.scalalogging.StrictLogging

object MainApp extends App with StrictLogging {

  val supervisor = SupervisionStrategy {
    case ex: Throwable =>
      logger.error(s"Unhandled error: ${ex.getMessage}", ex)
      ex.printStackTrace
      Supervision.Resume
  }

  implicit val actorSystem = ActorSystem("ensime-responder")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher

  val flows = new Flows {
    val config = Configuration.load
  }

  val (cancellable, process) = flows.graph.run

  process.onComplete { c =>
    s"Process completed with status: $c"
    actorSystem.shutdown()
  }
}
