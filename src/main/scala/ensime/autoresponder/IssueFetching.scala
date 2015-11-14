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
import com.typesafe.scalalogging.StrictLogging
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.util.{ Try, Success, Failure }
import scala.concurrent._
import scala.concurrent.duration._

trait IssueFetching {
  self: Environment with Transport with StrictLogging =>

  private val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z")

  private lazy val authHeaders =
    List(Authorization(GenericHttpCredentials("token", config.credentials.accessToken)))

  private val parallelism = 4

  private def issuesUrl(since: DateTime = DateTime.now) = {
    val sinceString = since.withZone(DateTimeZone.UTC).toString(dateTimeFormat)
    Uri(s"/repos/${config.owner}/${config.repo}/issues") //?since=$sinceString")
      .withHost(Host)
      .withScheme(Uri.httpScheme(securedConnection = true))
  }

  def tickSource: Source[Unit, Cancellable] =
    Source(1.second, config.pollInterval, ())

  def issueSource(implicit mat: Materializer, as: ActorSystem, ec: ExecutionContext): Source[Issue, Cancellable] =
    tickSource
      .map { unit =>
        val url = issuesUrl()
        logger.debug("About to fetch issues: " + url)
        HttpRequest(uri = url, headers = authHeaders) -> unit
      }
      .via(pool[Unit])
      .mapAsync(parallelism)(parseIssuesResponse)
      .mapConcat(_.to[collection.immutable.Iterable])
      .map { issue =>
        logger.debug(s"Found issue: $issue")
        issue
      }

  def parseIssuesResponse(input: (Try[HttpResponse], Unit))(implicit mat: Materializer, ec: ExecutionContext): Future[Seq[Issue]] = input match {
    case (Success(response), _) =>
      response.entity
        .toStrict(config.timeout)
        .map { entity =>
          Json.parse(entity.data.decodeString("UTF-8")).as[Seq[Issue]]
        }

    case _ =>
      // Ignore errors because YOLO:
      Future.successful(Seq.empty)
  }
}
