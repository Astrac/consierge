package ensime.autoresponder

import akka.actor.{ ActorSystem, Cancellable }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import scala.concurrent.duration._
import scala.util.Try

case class Credentials(
  username: String,
  accessToken: String)

case class Configuration(
  owner: String,
  repo: String,
  message: String,
  credentials: Credentials,
  pollInterval: FiniteDuration,
  timeout: FiniteDuration)

object Configuration {
  def load: Configuration = {
    val config = com.typesafe.config.ConfigFactory.load()

    def string(key: String): String =
      Option(config.getString(key))
        .getOrElse(sys.error(s"Config key not found: $key"))

    def duration(key: String): FiniteDuration =
      Option(config.getDuration(key))
        .map(_.toMillis.milliseconds)
        .getOrElse(sys.error(s"Config key not found: $key"))

    def file(key: String): String =
      scala.io.Source.fromFile(string(key)).mkString

    Configuration(
      owner = string("autoresponder.owner"),
      repo = string("autoresponder.repo"),
      message = file("autoresponder.messageFile"),
      credentials = Credentials(
        username = string("autoresponder.credentials.username"),
        accessToken = string("autoresponder.credentials.accessToken")
      ),
      pollInterval = duration("autoresponder.pollInterval"),
      timeout = duration("autoresponder.timeout")
    )
  }
}

trait Environment {
  def config: Configuration
}

trait Transport {
  val Host = "api.github.com"
  val DownloadTimeout = 500.millis
  val DownloadParallelism = 20

  def pool[T](implicit mat: Materializer, as: ActorSystem): Flow[(HttpRequest, T), (Try[HttpResponse], T), Unit] = Http().superPool[T]()
}
