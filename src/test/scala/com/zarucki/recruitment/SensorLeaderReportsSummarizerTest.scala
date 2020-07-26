package com.zarucki.recruitment

import java.io.File

import akka.actor.ActorSystem
import com.zarucki.recruitment.csv.CSVFormat
import com.zarucki.recruitment.sensing.{SensorId, SensorLeaderReport, SensorMeasurement, SensorStats}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

class SensorLeaderReportsSummarizerTest extends AnyFlatSpec with Matchers with ScalaFutures {

  implicit val actorSystem = ActorSystem()
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(5 seconds, 200 milliseconds)

  val csvFormatForSensorMeasurement = implicitly[CSVFormat[SensorMeasurement]]

  behavior of "SensorLeaderReportsSummarizer"

  it should "return empty report if no measurements" in {
    SensorLeaderReportsSummarizer.reportFromMeasurements(LazyList.empty) shouldEqual SensorLeaderReport()
  }

  it should "count invalid " in {
    SensorLeaderReportsSummarizer.reportFromMeasurements(
      LazyList(
        SensorMeasurement(SensorId("s1"), None),
        SensorMeasurement(SensorId("s1"), None)
      )
    ) shouldEqual SensorLeaderReport(Map(SensorId("s1") -> None), 0, 2, 2)
  }

  it should "calculate avg as double" in {
    SensorLeaderReportsSummarizer
      .reportFromMeasurements(
        LazyList(
          SensorMeasurement(SensorId("s1"), Some(10)),
          SensorMeasurement(SensorId("s1"), Some(11))
        )
      )
      .sensorAggregatedStats shouldEqual Map(SensorId("s1") -> Some(SensorStats(10, 10.5, 11, 2)))
  }

  it should "properly serialize report sorting values descending by avg then NaN" in {
    SensorLeaderReportsSummarizer.serializeReport(
      SensorLeaderReport(
        Map(
          SensorId("s1") -> Some(SensorStats(10)),
          SensorId("s2") -> None,
          SensorId("s3") -> Some(
            SensorStats(minHumidity = 3, cumulativeAvgHumidity = 12.2, maxHumidity = 30, numberOfValidSamples = 10)
          )
        ),
        mergedReports = 2,
        measurementTotal = 13,
        invalidMeasurementsCount = 2
      )
    ) shouldEqual
      """Num of processed files: 2
      |Num of processed measurements: 13
      |Num of failed measurements: 2
      |
      |Sensors with highest avg humidity:
      |
      |sensor-id,min,avg,max
      |s3,3,12.2,30
      |s1,10,10,10
      |s2,NaN,NaN,NaN
      |""".stripMargin
  }

  it should "properly report for task example data" in {
    SensorLeaderReportsSummarizer
      .summarizeForDirectory(new File("./data"), 4)
      .futureValue shouldEqual SensorLeaderReport(
      Map(
        SensorId("s3") -> None,
        SensorId("s2") -> Some(
          SensorStats(minHumidity = 78, cumulativeAvgHumidity = 82.0, maxHumidity = 88, numberOfValidSamples = 3)
        ),
        SensorId("s1") -> Some(
          SensorStats(minHumidity = 10, cumulativeAvgHumidity = 54.0, maxHumidity = 98, numberOfValidSamples = 2)
        )
      ),
      mergedReports = 2,
      measurementTotal = 7,
      invalidMeasurementsCount = 2
    )
  }

  it should "throw exception when directory is not existing" in {
    SensorLeaderReportsSummarizer
      .summarizeForDirectory(new File("./data-non-existing"), 1)
      .failed
      .futureValue
      .isInstanceOf[IllegalArgumentException] shouldEqual true
  }

  it should "properly parse csv line with NaN as None" in {
    csvFormatForSensorMeasurement.parse("s1,NaN") shouldEqual Success(
      SensorMeasurement(SensorId("s1"), None)
    )
  }

  it should "properly parse csv line with humidity" in {
    csvFormatForSensorMeasurement.parse("s3,40") shouldEqual Success(
      SensorMeasurement(SensorId("s3"), Some(40))
    )
  }

  it should "return failure if humidity is not a integer" in {
    csvFormatForSensorMeasurement.parse("s3,adsf").isFailure shouldEqual true
  }

  it should "properly combine two reports" in {
    val report1 = SensorLeaderReportsSummarizer.reportFromMeasurements(
      LazyList(
        SensorMeasurement(SensorId("s1"), Some(1)),
        SensorMeasurement(SensorId("s1"), Some(2)),
        SensorMeasurement(SensorId("s2"), Some(5))
      )
    )

    val report2 = SensorLeaderReportsSummarizer.reportFromMeasurements(
      LazyList(
        SensorMeasurement(SensorId("s1"), None),
        SensorMeasurement(SensorId("s2"), Some(1)),
        SensorMeasurement(SensorId("s1"), Some(9))
      )
    )

    SensorLeaderReport.combineReports(report1, report2) shouldEqual SensorLeaderReport(
      Map(
        SensorId("s1") -> Some(
          SensorStats(minHumidity = 1, cumulativeAvgHumidity = 4.0, maxHumidity = 9, numberOfValidSamples = 3)
        ),
        SensorId("s2") -> Some(
          SensorStats(minHumidity = 1, cumulativeAvgHumidity = 3.0, maxHumidity = 5, numberOfValidSamples = 2)
        )
      ),
      mergedReports = 2,
      measurementTotal = 6,
      invalidMeasurementsCount = 1
    )
  }
}
