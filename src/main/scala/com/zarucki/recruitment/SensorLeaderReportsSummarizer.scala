package com.zarucki.recruitment

import java.io.File
import java.text.DecimalFormat

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import com.zarucki.recruitment.csv.CSVReader
import com.zarucki.recruitment.sensing.{SensorLeaderReport, SensorMeasurement}

import scala.concurrent.Future

object SensorLeaderReportsSummarizer extends LazyLogging {
  private val decimalFormat = new DecimalFormat("#.##")
  private val nanString = "NaN"

  def summarizeForDirectory(dirFile: File, parallelism: Int)(
      implicit actorSystem: ActorSystem
  ): Future[SensorLeaderReport] = {
    if (dirFile.isDirectory) {
      val fileList = dirFile.listFiles((_: File, name: String) => name.endsWith(".csv")).toList

      Source
        .fromIterator(() => fileList.iterator)
        .mapAsyncUnordered(parallelism) { csvFile =>
          Future.successful(reportFromMeasurements(new CSVReader(csvFile).parsedAs[SensorMeasurement]))
        }
        .runFold(SensorLeaderReport())(SensorLeaderReport.combineTwoReports)
    } else {
      Future.failed(new IllegalArgumentException("Given path is not directory"))
    }
  }

  def reportFromMeasurements(measurements: LazyList[SensorMeasurement]): SensorLeaderReport = {
    measurements.foldLeft(SensorLeaderReport())((report, measurement) => report.addSample(measurement))
  }

  def serializeReport(sensorLeaderReport: SensorLeaderReport): String = {
    val sensorSummarySortedDesc = sensorLeaderReport.sensorAggregatedStats.toList
      .sortBy { case (id, maybeStats) => (maybeStats.map(-_.cumulativeAvgHumidity).getOrElse(Double.NaN), id) }
      .map {
        case (sensorId, Some(stats)) =>
          val formattedAvgHumidity = decimalFormat.format(stats.cumulativeAvgHumidity)
          s"$sensorId,${stats.minHumidity},$formattedAvgHumidity,${stats.maxHumidity}"
        case (sensorId, None) => s"$sensorId,$nanString,$nanString,$nanString"
      }
      .mkString("\n")

    s"""Num of processed files: ${sensorLeaderReport.mergedReports}
       |Num of processed measurements: ${sensorLeaderReport.measurementTotal}
       |Num of failed measurements: ${sensorLeaderReport.invalidMeasurementsCount}
       |
       |Sensors with highest avg humidity:
       |
       |sensor-id,min,avg,max
       |${sensorSummarySortedDesc}
       |""".stripMargin
  }
}
