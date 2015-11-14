package ensime.autoresponder

import akka.actor.{ ActorSystem, Cancellable }
import akka.stream.Materializer
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.{ ExecutionContext, Future }

trait Flows extends CommentSubmission with IssueFetching with IssueFilter with StrictLogging {
  def responseSink: Sink[CommentResponse, Future[Unit]] = Sink.foreach[CommentResponse](resp =>
    logger.info(s"Submitted a new comment: $resp"))

  def graph(implicit mat: Materializer, as: ActorSystem, ec: ExecutionContext) = issueSource
    .via(filterFlow)
    .via(respondFlow)
    .toMat(responseSink)(Keep.both)
}
