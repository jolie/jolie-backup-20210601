

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._


class BasicSimulation extends Simulation {

  val httpProtocol = http
                      .baseUrl("http://localhost") // Here is the root for all relative URLs
                      .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
                      .acceptEncodingHeader("gzip, deflate")
                      .acceptLanguageHeader("en-US,en;q=0.5")
  val scn = scenario("Test Jolie") // A scenario is a chain of requests and pauses
        .exec(http("request_1")
          .post("/")
          .body(ElFileBody("data.json")).asJson)
  setUp(scn.inject(atOnceUsers(10),
                   rampUsers(10) during (5 seconds), // 3
                   constantUsersPerSec(20) during (15 seconds), // 4
                   constantUsersPerSec(20) during (15 seconds) randomized, // 5
                   rampUsersPerSec(10) to 20 during (1 minutes), // 6
                   rampUsersPerSec(10) to 20 during (1 minutes) randomized, // 7
                   heavisideUsers(1000) during (20 seconds)).protocols(httpProtocol))

}