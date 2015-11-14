package ensime.autoresponder

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import org.joda.time.LocalDateTime
import scala.util.Try

sealed trait IssueState
object IssueState {
  case object Open extends IssueState
  case object Closed extends IssueState
}

case class User(id: Int)

case class Issue(id: Int, user: User, title: String, body: String, state: IssueState, createdAt: LocalDateTime, updatedAt: LocalDateTime)

case class CommentResponse(id: Int, url: String, createdAt: LocalDateTime)

trait Flows {
  def issueSource: Source[Issue, Unit]
  def respondFlow: Flow[Issue, CommentResponse, Unit] // Posts a message to the issue
  def responseSink: Sink[CommentResponse, Unit]

  def makeRequest(i: Issue): HttpRequest = ???
  def handleResponse(r: (Try[HttpResponse], Issue)): Issue = ???

  def pool[T](implicit mat: Materializer, as: ActorSystem) = Http().superPool[T]()

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
