package controllers

import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.libs.streams._
import play.api.libs.ws._
import play.api.mvc._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class MyController @Inject()(ws: WSClient, val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext) extends BaseController {

  def forward(request: WSRequest): BodyParser[WSResponse] = BodyParser { req =>
    Accumulator.source[ByteString].mapFuture { source =>
      request
        .withBody(source)
        .execute()
        .map(Right.apply)
    }
  }

  def myAction = Action(forward(ws.url("https://denniskleine.s3.eu-central-1.amazonaws.com/politicalspeeches.csv"))) { req =>
    Ok("Uploaded")
  }

  // val Action = inject[DefaultActionBuilder]

  val csv: BodyParser[Seq[Seq[String]]] = BodyParser { req =>
    // A flow that splits the stream into CSV lines
    val sink: Sink[ByteString, Future[Seq[Seq[String]]]] = Flow[ByteString]
      // We split by the new line character, allowing a maximum of 1000 characters per line
      .via(Framing.delimiter(ByteString("\n"), 1000, allowTruncation = true))
      // Turn each line to a String and split it by commas
      .map(_.utf8String.trim.split(",").toSeq)
      // Now we fold it into a list
      .toMat(Sink.fold(Seq.empty[Seq[String]])(_ :+ _))(Keep.right)

    // Convert the body to a Right either
    Accumulator(sink).map(Right.apply)
  }
}

