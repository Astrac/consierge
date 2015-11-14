package ensime.autoresponder

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object MainApp extends App {
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
