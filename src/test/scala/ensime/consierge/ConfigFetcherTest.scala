package ensime.consierge

import org.scalatest.{FlatSpec, Matchers}
import scala.concurrent.duration._

class ConfigFetcherTest extends FlatSpec with Matchers {

  "config" should "load all data from test file" in {
    val config = Configuration.load
    assert(config.owner === "user_name")
    assert(config.repo === "repo_name")
    assert(config.credentials.username === "login_name")
    assert(config.pollInterval === 10.seconds)

    assert(config.message.nonEmpty === true)
  }

}
