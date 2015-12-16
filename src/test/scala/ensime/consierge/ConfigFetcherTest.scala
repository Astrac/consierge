package ensime.consierge

import org.scalatest.{FlatSpec, Matchers}
import scala.concurrent.duration._

class ConfigFetcherTest extends FlatSpec with Matchers {

  "config" should "load all data from test file" in {
    val config = Configuration.load
    config.owner should equal ("user_name")
    config.owner  should equal ("user_name")
    config.repo  should equal ("repo_name")
    config.credentials.username should equal ("login_name")
    config.pollInterval should equal (10.seconds)

    config.message.nonEmpty should be (true)
  }

}
