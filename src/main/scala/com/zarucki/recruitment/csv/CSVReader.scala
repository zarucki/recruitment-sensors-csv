package com.zarucki.recruitment
package csv

import java.io.File

import com.typesafe.scalalogging.LazyLogging

import scala.util.Try

class CSVReader(file: File, withCSVHeader: Boolean = true) extends LazyLogging {

  def parsedAs[T](implicit format: CSVFormat[T]): LazyList[T] = {
    Try(scala.io.Source.fromFile(file))
      .map { stream =>
        stream
          .getLines()
          .to(LazyList)
          .drop(if (withCSVHeader) 1 else 0)
          .map { line =>
            logger.debug(s"Parsing line ${line} from file ${file}")
            format.parse(line).onNonFatalFailure(logger.warn(s"Error while parsing line: '${line}'", _: Throwable))
          }
          .flatMap(_.toOption)
      }
      .onNonFatalFailure(logger.warn(s"Error trying to parse file ${file}", _: Throwable))
      .getOrElse(LazyList.empty)
  }

}
