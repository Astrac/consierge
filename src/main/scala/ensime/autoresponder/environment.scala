package ensime.autoresponder

import akka.actor.ActorSystem
import akka.actor.Cancellable
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.headers.GenericHttpCredentials
import akka.stream.ActorMaterializer
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.util.{ Try, Success, Failure }
import scala.concurrent._
import scala.concurrent.duration._

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
        .map(_.getNano.nanoseconds)
        .getOrElse(sys.error(s"Config key not found: $key"))

    def file(key: String): String =
      scala.io.Source.fromFile(string(key)).mkString

    Configuration(
      owner = string("autoresponder.owner"),
      repo = string("autoresponder.repo"),
      message = file("autoresponder.messageFile"),
      credentials = Credentials(
        username = string("autoresponder.credentials.username"),
        accessToken = string("autoresponder.credentials.accessToken")),
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

  def pool[T](implicit mat: Materializer, as: ActorSystem): Flow[(HttpRequest, T), (Try[HttpResponse], T), Unit] = Http().superPool[T]()
}
