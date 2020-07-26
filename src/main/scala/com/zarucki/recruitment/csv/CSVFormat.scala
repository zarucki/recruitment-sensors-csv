package com.zarucki.recruitment.csv

import scala.util.Try

trait CSVFormat[T] {
  def parse(line: String): Try[T]
}
