package ensime.consierge

import akka.actor.{ ActorSystem, Cancellable }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, Uri }
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.StrictLogging
import org.joda.time.{ DateTime, DateTimeZone }
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{ Success, Try }

trait IssueFetching {
  self: Environment with Transport with StrictLogging =>

  private val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z")

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
        HttpRequest(uri = url) -> unit
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
          val dt = entity.data.decodeString("UTF-8")
          try {
            Json.parse(dt).as[Seq[Issue]]
          } catch {
            case e: Exception =>
              logger.error(s"Unable to parse server results: $dt")
              throw e
          }
        }

    case _ =>
      // Ignore errors because YOLO:
      Future.successful(Seq.empty)
  }
}
