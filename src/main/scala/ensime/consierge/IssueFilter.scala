package ensime.consierge

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.Uri
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

trait IssueFilter extends Transport with Environment with StrictLogging {

  def contributorsUri(i: Issue) = s"/repos/${config.owner}/${config.repo}/contributors"

  def contributorsRequest(i: Issue): HttpRequest = HttpRequest(
    method = HttpMethods.GET,
    uri = Uri(contributorsUri(i))
      .withHost(Host)
      .withScheme(Uri.httpScheme(securedConnection = true))
  )

  def contributorsResponse(r: Try[HttpResponse], i: Issue)(implicit mat: Materializer, ec: ExecutionContext): Future[(Issue, Boolean)] = r match {
    case Success(resp) =>
      resp.entity.toStrict(DownloadTimeout).map { e =>
        (i, !Json.parse(e.data.decodeString("UTF-8")).as[Seq[User]].exists(_.id == i.user.id))
      }
    case Failure(ex) => throw ex
  }

  def commentsUri(i: Issue) = s"/repos/${config.owner}/${config.repo}/issues/${i.number}/comments"

  def commentsRequest(i: Issue): HttpRequest = HttpRequest(
    method = HttpMethods.GET,
    uri = Uri(commentsUri(i))
      .withHost(Host)
      .withScheme(Uri.httpScheme(securedConnection = true))
  )

  def commentsResponse(r: Try[HttpResponse], i: Issue)(implicit mat: Materializer, ec: ExecutionContext): Future[(Issue, Boolean)] = r match {
    case Success(resp) =>
      resp.entity.toStrict(DownloadTimeout).map { e =>
        (i, Json.parse(e.data.decodeString("UTF-8")).as[Seq[IssueComment]].forall(_.user.login != config.credentials.username))
      }
    case Failure(ex) => throw ex
  }

  def contributorFilterIfEnabled(implicit mat: Materializer, as: ActorSystem, ec: ExecutionContext)  =
    if  (config.fetchOpts.contributorFilter) contributorFilter else Flow[Issue].map(i => i)

  def contributorFilter(implicit mat: Materializer, as: ActorSystem, ec: ExecutionContext) = Flow[Issue]
    .map(i => (contributorsRequest(i), i))
    .via(pool)
    .mapAsync(DownloadParallelism)((contributorsResponse _).tupled)
    .map {
      case (issue, shouldComment) =>
        logger.debug(s"Am I a contributor for issue ${issue.id}? $shouldComment")
        (issue, shouldComment)
    }
    .filter(_._2)
    .map(_._1)

  def commentedFilter(implicit mat: Materializer, as: ActorSystem, ec: ExecutionContext) = Flow[Issue]
    .map(i => (commentsRequest(i), i))
    .via(pool)
    .mapAsync(DownloadParallelism)((commentsResponse _).tupled)
    .map {
      case (issue, shouldComment) =>
        logger.debug(s"Have I commented on issue ${issue.id}? $shouldComment")
        (issue, shouldComment)
    }
    .filter(_._2)
    .map(_._1)

  def filterFlow(implicit mat: Materializer, as: ActorSystem, ec: ExecutionContext): Flow[Issue, Issue, Unit] =
    Flow[Issue]
      .via(contributorFilterIfEnabled)
      .via(commentedFilter)
}
