package com.zarucki.recruitment.sensing

case class SensorStats(
    minHumidity: Int,
    cumulativeAvgHumidity: Double,
    maxHumidity: Int,
    numberOfValidSamples: Int
) {
  require(maxHumidity >= minHumidity)

  def withNewSample(newHumidity: Int): SensorStats = {
    SensorStats(
      minHumidity = Math.min(minHumidity, newHumidity),
      cumulativeAvgHumidity = SensorStats.cumulativeAvg(cumulativeAvgHumidity, numberOfValidSamples, newHumidity),
      maxHumidity = Math.max(maxHumidity, newHumidity),
      numberOfValidSamples = numberOfValidSamples + 1
    )
  }
}

object SensorStats {
  def apply(humidity: Int): SensorStats = {
    SensorStats(
      minHumidity = humidity,
      maxHumidity = humidity,
      cumulativeAvgHumidity = humidity.toDouble,
      numberOfValidSamples = 1
    )
  }

  def cumulativeAvg(currentCumulativeAvg: Double, numberOfSamples: Int, newSample: Int): Double = {
    currentCumulativeAvg + (newSample - currentCumulativeAvg) / (numberOfSamples + 1.0)
  }

  def combineStatsForSensor(stats1: SensorStats, stats2: SensorStats): SensorStats = {
    SensorStats(
      minHumidity = Math.min(stats1.minHumidity, stats2.minHumidity),
      cumulativeAvgHumidity = combineCumulativeAvgs(
        stats1.cumulativeAvgHumidity,
        stats1.numberOfValidSamples,
        stats2.cumulativeAvgHumidity,
        stats2.numberOfValidSamples
      ),
      maxHumidity = Math.max(stats1.maxHumidity, stats2.maxHumidity),
      numberOfValidSamples = stats1.numberOfValidSamples + stats2.numberOfValidSamples
    )
  }

  // TODO: calculation errors?
  private def combineCumulativeAvgs(
      cumulativeAvg1: Double,
      numberOfSamples1: Int,
      cumulativeAvg2: Double,
      numberOfSamples2: Int
  ): Double = {
    ((cumulativeAvg1 * numberOfSamples1) + (cumulativeAvg2 * numberOfSamples2)) / (numberOfSamples1 + numberOfSamples2)
  }
}
