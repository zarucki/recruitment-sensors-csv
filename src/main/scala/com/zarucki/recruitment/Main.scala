package com.zarucki.recruitment

import java.io.File

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory

object Main extends App with LazyLogging {
  val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]

  if (args.map(_.toLowerCase).contains("--verbatim")) {
    rootLogger.setLevel(Level.DEBUG)
  } else {
    rootLogger.setLevel(Level.WARN)
  }

  if (args.isEmpty) {
    println("Program needs directory as first argument")
  } else {
    val dirPath = args(0)

    implicit val actorSystem = ActorSystem()
    import actorSystem.dispatcher

    SensorLeaderReportsSummarizer
      .summarizeForDirectory(new File(dirPath), parallelism = 10)
      .map(SensorLeaderReportsSummarizer.serializeReport)
      .map(print)
      .flatMap(_ => actorSystem.terminate())
      .recoverWith {
        case t =>
          println(s"Error while reading directory ${dirPath}: ${t.getMessage}.")
          actorSystem.terminate()
      }
  }
}
