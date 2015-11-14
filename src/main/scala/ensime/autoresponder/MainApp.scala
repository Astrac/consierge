package ensime.autoresponder

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.stream.Supervision
import com.typesafe.scalalogging.StrictLogging

object MainApp extends App with StrictLogging {

  val supervisor: (Throwable => Supervision.Directive) = {
    case ex: Throwable =>
      logger.error(s"Unhandled error: ${ex.getMessage}", ex)
      ex.printStackTrace
      Supervision.Resume
  }

  implicit val actorSystem = ActorSystem("ensime-responder")
  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(actorSystem) withSupervisionStrategy supervisor)
  implicit val executionContext = actorSystem.dispatcher

  val flows = new Flows {
    val config = Configuration.load
  }

  logger.debug(s"Configuration: ${flows.config}")

  val (cancellable, process) = flows.graph.run

  process.onComplete { c =>
    logger.info(s"Process completed with status: $c")
    actorSystem.shutdown()
  }

  actorSystem.awaitTermination()
}
