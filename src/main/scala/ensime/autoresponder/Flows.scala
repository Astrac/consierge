package ensime.autoresponder

import akka.actor.ActorSystem
import akka.actor.Cancellable
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri }
import akka.stream.Materializer
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import org.joda.time.DateTime
import play.api.libs.json.{ JsValue, Json }
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

trait Flows extends CommentSubmission with IssueFetching with StrictLogging {
  def searchFlow(implicit mat: Materializer, as: ActorSystem): Flow[Issue, Issue, Unit]

  def responseSink: Sink[CommentResponse, Future[Unit]] = Sink.foreach[CommentResponse](resp =>
    logger.info(s"Submitted a new comment: $resp")
  )

  def graph(implicit mat: Materializer, as: ActorSystem, ec: ExecutionContext) = issueSource
    .via(searchFlow)
    .via(respondFlow)
    .to(responseSink)
}
