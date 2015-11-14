package ensime.autoresponder

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import play.api.libs.json.{ JsValue, Json }
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

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
      data = ByteString(Json.stringify(makeJson(i)), "UTF-8")
    )
  )

  def commentResponse(r: Try[HttpResponse], i: Issue)(implicit mat: Materializer, ec: ExecutionContext): Future[CommentResponse] = r match {
    case Success(resp) =>
      resp.entity.toStrict(DownloadTimeout).map { e =>
        Json.parse(e.data.decodeString("UTF-8")).as[CommentResponse]
      }
    case Failure(ex) => throw ex
  }

  def respondFlow(implicit mat: Materializer, as: ActorSystem, ec: ExecutionContext): Flow[Issue, CommentResponse, Unit] = Flow[Issue]
    .map(i => (commentRequest(i), i))
    .via(pool)
    .mapAsync(DownloadParallelism)((commentResponse _).tupled)

}
