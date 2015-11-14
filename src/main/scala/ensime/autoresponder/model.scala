package ensime.autoresponder

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.Uri
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.joda.time.LocalDateTime
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import scala.util.Try

case class Configuration(owner: String, repo: String, message: String)

sealed trait IssueState
object IssueState {
  case object Open extends IssueState
  case object Closed extends IssueState
}

case class User(id: Int)

case class Issue(id: Int, number: Int, user: User, title: String, body: String, state: IssueState, createdAt: LocalDateTime, updatedAt: LocalDateTime)

case class CommentResponse(id: Int, url: String, createdAt: LocalDateTime)

trait Environment {
  def config: Configuration
}

trait Flows extends CommentSubmission {
  def issueSource: Source[Issue, Unit]
  def responseSink: Sink[CommentResponse, Unit]

  def makeRequest(i: Issue): HttpRequest = ???
  def handleResponse(r: (Try[HttpResponse], Issue)): Issue = ???

  def searchFlow(implicit mat: Materializer, as: ActorSystem) =
    Flow[Issue]
      .map(i => (makeRequest(i), i))
      .via(pool)
      .map(handleResponse)

  def graph(implicit mat: Materializer, as: ActorSystem) = issueSource
    .via(searchFlow)
    .via(respondFlow)
    .to(responseSink)
}

trait CommentSubmission extends Transport with Environment {
  def createCommentUri(i: Issue) = s"/repos/${config.owner}/${config.repo}/issues/${i.number}/comments"

  def makeJson(i: Issue): JsValue = Json.obj("body" -> config.message)

  def commentRequest(i: Issue): HttpRequest = HttpRequest(
    method = HttpMethods.POST,
    uri = Uri(createCommentUri(i))
      .withHost(Host)
      .withScheme(Uri.httpScheme(securedConnection = true)),
    entity = HttpEntity.apply(
      contentType = ContentTypes.`application/json`,
      data = ByteString.apply(Json.stringify(makeJson(i)), "UTF-8"))
  )

  def commentResponse(r: Try[HttpResponse], i: Issue): CommentResponse

  def respondFlow(implicit mat: Materializer, as: ActorSystem): Flow[Issue, CommentResponse, Unit] = Flow[Issue]
    .map(i => (commentRequest(i), i))
    .via(pool)
    .map((commentResponse _).tupled)

}

trait Transport {
  val Host = "api.github.com"

  def pool[T](implicit mat: Materializer, as: ActorSystem): Flow[(HttpRequest, T), (Try[HttpResponse], T), Unit] = Http().superPool[T]()
}
