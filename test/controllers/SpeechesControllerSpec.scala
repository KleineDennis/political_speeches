package controllers

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}

import scala.concurrent.Future

class SpeechesControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "SpeechesController GET" should {

    "evaluate data from given URL" in {
      val request = FakeRequest(GET, "/evaluation?url=https://denniskleine.s3.eu-central-1.amazonaws.com/politicalspeeches.csv").withHeaders(HOST -> "localhost:9000")
      val result: Future[Result] = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val stats: Statistic = Json.fromJson[Statistic](contentAsJson(result)).get
      stats mustBe Statistic(null, "Alexander Abel", "Caesare Collins")
    }

  }
}
