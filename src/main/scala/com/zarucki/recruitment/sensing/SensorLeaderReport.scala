package com.zarucki.recruitment.sensing

case class SensorLeaderReport(
    sensorAggregatedStats: Map[SensorId, Option[SensorStats]] = Map.empty, // None means only NaNs measurement for sensor
    mergedReports: Int = 0,
    measurementTotal: Int = 0,
    invalidMeasurementsCount: Int = 0,
) {
  def addSample(sensorMeasurement: SensorMeasurement): SensorLeaderReport = {
    val sensorId = sensorMeasurement.sensorId
    sensorMeasurement.humidity match {
      case Some(newHumidity) =>
        sensorAggregatedStats.get(sensorId).flatten match {
          case Some(existingStats) =>
            copy(
              sensorAggregatedStats = sensorAggregatedStats
                .updated(sensorId, Some(existingStats.withNewSample(newHumidity))),
              measurementTotal = measurementTotal + 1
            )
          case None =>
            copy(
              sensorAggregatedStats = sensorAggregatedStats.updated(sensorId, Some(SensorStats(newHumidity))),
              measurementTotal = measurementTotal + 1,
            )
        }
      case None =>
        sensorAggregatedStats.get(sensorId) match {
          case None =>
            copy(
              sensorAggregatedStats = sensorAggregatedStats.updated(sensorId, None),
              measurementTotal = measurementTotal + 1,
              invalidMeasurementsCount = invalidMeasurementsCount + 1
            )
          case _ =>
            copy(
              measurementTotal = measurementTotal + 1,
              invalidMeasurementsCount = invalidMeasurementsCount + 1
            )
        }
    }
  }
}

object SensorLeaderReport {
  def combineReports(reports: SensorLeaderReport*): SensorLeaderReport = {
    SensorLeaderReport(
      reports
        .flatMap(_.sensorAggregatedStats.toList)
        .groupBy { case (sensorId, _) => sensorId }
        .map {
          case (sensorId, group: Seq[(SensorId, Option[SensorStats])]) => // Intellij has problems inferring it
            val statsToMerge = group.flatMap { case (_, maybeStats) => maybeStats }
            val mergedStats = statsToMerge.foldLeft[Option[SensorStats]](None) {
              case (Some(a), b) => Some(SensorStats.combineStatsForSensor(a, b))
              case (None, b)    => Some(b)
            }
            (sensorId, mergedStats)
        }
        .toMap,
      mergedReports = reports.size,
      measurementTotal = reports.map(_.measurementTotal).sum,
      invalidMeasurementsCount = reports.map(_.invalidMeasurementsCount).sum
    )
  }

  def combineTwoReports(report1: SensorLeaderReport, report2: SensorLeaderReport): SensorLeaderReport =
    combineReports(report1, report2)
}
