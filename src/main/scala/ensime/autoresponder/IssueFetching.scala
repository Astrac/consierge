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

trait IssueFetching {
  self: Environment with Transport =>

  implicit val userReads: Reads[User] =
    (__ \ "id").read[Int].map(User.apply)

  implicit object issueStateReads extends Reads[IssueState] {
    def reads(json: JsValue) = json match {
      case JsString("open") => JsSuccess(IssueState.Open)
      case JsString("closed") => JsSuccess(IssueState.Closed)
      case other => JsError("bad.issue.state: " + other)
    }
  }

  implicit val issueReads: Reads[Issue] = (
    (__ \ "id").read[Int] ~
    (__ \ "number").read[Int] ~
    (__ \ "user").read[User] ~
    (__ \ "title").read[String] ~
    (__ \ "body").read[String] ~
    (__ \ "state").read[IssueState] ~
    (__ \ "created_at").read[String].map(DateTime.parse) ~
    (__ \ "updated_at").read[String].map(DateTime.parse)
  )(Issue.apply _)

  private lazy val issuesUrl =
    s"https://api.github.com/repos/${config.owner}/${config.repo}/issues"

  private lazy val authHeaders =
    List(Authorization(GenericHttpCredentials("token", config.accessToken)))

  private lazy val parallelism = 4

  def tickSource: Source[Unit, Cancellable] =
    Source(1.second, config.pollInterval, ())

  def issueSource(implicit mat: Materializer, as: ActorSystem, ec: ExecutionContext): Source[Issue, Cancellable] =
    tickSource
      .map(unit => HttpRequest(uri = issuesUrl, headers = authHeaders) -> unit)
      .via(pool[Unit])
      .mapAsync(parallelism)(parseIssuesResponse)
      .mapConcat(_.to[collection.immutable.Iterable])

  def parseIssuesResponse(input: (Try[HttpResponse], Unit))(implicit mat: Materializer, ec: ExecutionContext): Future[Seq[Issue]] = input match {
    case (Success(response), _) =>
      response.entity
        .toStrict(config.timeout)
        .map { entity =>
          try {
            val json = entity.data.decodeString("UTF-8")
            Json.fromJson[Seq[Issue]](Json.parse(json)).fold(
              invalid = { errors => sys.error("BADNESS " + errors) },
              valid = { issues => issues }
            )
          } catch {
            case exn: Throwable =>
              println("ERRORS " + exn)
              Nil
          }
        }

    case _ =>
      // Ignore errors because YOLO:
      Future.successful(Seq.empty)
  }
}
