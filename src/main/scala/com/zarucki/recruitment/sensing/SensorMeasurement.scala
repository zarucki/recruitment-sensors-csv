package com.zarucki.recruitment.sensing

import com.zarucki.recruitment.csv.CSVFormat

import scala.util.Try

// None is NaN measurement
case class SensorMeasurement(sensorId: SensorId, humidity: Option[Int])

object SensorMeasurement {
  implicit val csvFormat = new CSVFormat[SensorMeasurement] {
    override def parse(line: String): Try[SensorMeasurement] = Try {
      val splitLine = line.split(',')
      val id = splitLine(0)
      val humidity = if (splitLine(1).toLowerCase.stripMargin == "nan") {
        None
      } else {
        Some(splitLine(1).toInt)
      }
      SensorMeasurement(SensorId(id), humidity)
    }
  }
}
