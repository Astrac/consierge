package ensime.consierge

import akka.actor.{ ActorSystem, Cancellable }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.headers.GenericHttpCredentials
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.duration._
import scala.util.Try
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

case class Credentials(
  username: String,
  accessToken: String)

case class FetchConfig(contributorFilter: Boolean = true, sinceEnabled: Boolean = true)

case class Configuration(
  owner: String,
  repo: String,
  messageFile: String,
  messageOption: Option[String],
  credentials: Credentials,
  pollInterval: FiniteDuration,
  timeout: FiniteDuration,
  fetchOpts: FetchConfig) {

  def message = messageOption.getOrElse(sys.error("Message was not setup!"))

}

object Configuration {
  def load: Configuration = {
    val config = com.typesafe.config.ConfigFactory.load()

    val cfg = config.as[Configuration]("consierge")

    def file(key: String): String =
      scala.io.Source.fromFile(key).mkString

    cfg.copy(messageOption = Option(file(cfg.messageFile)))
  }
}

trait Environment {
  def config: Configuration
}

trait Transport extends StrictLogging {
  this: Environment =>

  val Host = "api.github.com"
  val DownloadTimeout = 500.millis
  val DownloadParallelism = 20

  private lazy val authHeaders: Iterable[HttpHeader] =
    List(Authorization(GenericHttpCredentials("token", config.credentials.accessToken)))

  def pool[T](implicit mat: Materializer, as: ActorSystem): Flow[(HttpRequest, T), (Try[HttpResponse], T), Unit] =
    Flow[(HttpRequest, T)]
      .map { req =>
        val authReq = (req._1.withHeaders(req._1.headers ++ authHeaders), req._2)
        logger.debug(s"Sending request: $authReq")
        authReq
      }
      .via(Http().superPool[T]())
}
