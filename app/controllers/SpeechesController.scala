package controllers

import play.api.Logger
import play.api.libs.json.{Format, Json}
import play.api.libs.ws._
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext


case class Speech(Speaker: String, topic: String, date: String, words: Int)

case class Statistic(mostSpeeches: String, mostSecurity: String, leastWordy: String)

object Statistic {
  implicit val format: Format[Statistic] = Json.format
}


@Singleton
class SpeechesController @Inject()(ws: WSClient, val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext) extends BaseController {
  private val logger = Logger(getClass)

  def evaluation(url: List[String]): Action[AnyContent] = Action.async {
    logger.info(s"evaluation: = $url")

    val uri = url.headOption.getOrElse("")

    // Get .csv files from all urls and merge data
    val statistic = ws.url(uri).get().map { maybeWSResponse =>
      val speeches = for {
        line <- maybeWSResponse.body.split("\n").toList
        Array(speaker, topic, date, words) = line.split(",").map(_.trim)
      } yield Speech(speaker, topic, date, words.toInt)

      // Get statistics from List
      val mostSpeechesIn2013 = speeches
        .filter(_.date.substring(0, 4) == "2013")
        .groupBy(_.Speaker)
        .map { case (k, v) => (k, v.length) }
        .maxByOption(_._2).map(_._1).orNull

      val mostSpeechesInTopicInternalSecurity = speeches
        .filter(_.topic.contains("Internal Security"))
        .groupBy(_.Speaker)
        .map { case (k, v) => (k, v.length) }
        .maxByOption(_._2).map(_._1).orNull

      val spokeTheFewestWords = speeches
        .groupMapReduce(_.Speaker)(_.words)(_ + _)
        .minByOption(_._2).map(_._1).orNull

      // Save statistics
      Statistic(mostSpeechesIn2013, mostSpeechesInTopicInternalSecurity, spokeTheFewestWords)
    }

    // Response in json format
    statistic.map { stat =>
      Ok(Json.toJson(stat))
    }
  }
}