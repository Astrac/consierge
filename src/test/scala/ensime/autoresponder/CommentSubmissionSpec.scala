package ensime.consierge

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import akka.util.ByteString
import org.joda.time.DateTime
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }
import scala.util.{ Success, Try }

class CommententSubmissionSpec extends FlatSpec with Matchers with ScalaFutures {
  implicit val actorSystem = ActorSystem("consierge-test")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit override val patienceConfig = PatienceConfig(Span(1, Seconds), Span(100, Millis))

  val configuration = Configuration("foo", "bar", "A message")

  val mockResponseBody = """
{
  "id": 1,
  "url": "https://api.github.com/repos/octocat/Hello-World/issues/comments/1",
  "html_url": "https://github.com/octocat/Hello-World/issues/1347#issuecomment-1",
  "body": "Me too",
  "user": {
    "login": "octocat",
    "id": 1,
    "avatar_url": "https://github.com/images/error/octocat_happy.gif",
    "gravatar_id": "",
    "url": "https://api.github.com/users/octocat",
    "html_url": "https://github.com/octocat",
    "followers_url": "https://api.github.com/users/octocat/followers",
    "following_url": "https://api.github.com/users/octocat/following{/other_user}",
    "gists_url": "https://api.github.com/users/octocat/gists{/gist_id}",
    "starred_url": "https://api.github.com/users/octocat/starred{/owner}{/repo}",
    "subscriptions_url": "https://api.github.com/users/octocat/subscriptions",
    "organizations_url": "https://api.github.com/users/octocat/orgs",
    "repos_url": "https://api.github.com/users/octocat/repos",
    "events_url": "https://api.github.com/users/octocat/events{/privacy}",
    "received_events_url": "https://api.github.com/users/octocat/received_events",
    "type": "User",
    "site_admin": false
  },
  "created_at": "2011-04-14T16:00:49Z",
  "updated_at": "2011-04-14T16:00:49Z"
}
  """

  val mockResponse = HttpResponse(
    status = StatusCodes.Created,
    entity = HttpEntity.Strict(
      ContentTypes.`application/json`, ByteString(mockResponseBody)
    )
  )

  def submission(resp: Try[HttpResponse]) = new CommentSubmission {
    override val config = configuration

    override def pool[T](implicit mat: Materializer, as: ActorSystem) =
      Flow[(HttpRequest, T)].map {
        case (_, t) => (resp, t)
      }
  }

  "The submission flow" should "transform an issue in a comment submission" in {
    val user = User(2)

    val issue = Issue(
      1,
      123,
      user,
      "An issue",
      "This is an issue",
      IssueState.Open,
      DateTime.now,
      DateTime.now
    )

    val resp = Source
      .single(issue)
      .via(submission(Success(mockResponse)).respondFlow)
      .toMat(Sink.head)(Keep.right)
      .run

    whenReady(resp) { lst =>
      lst should equal(
        CommentResponse(
          1,
          "https://api.github.com/repos/octocat/Hello-World/issues/comments/1",
          DateTime.parse("2011-04-14T16:00:49Z")
        )
      )
    }
  }
}
