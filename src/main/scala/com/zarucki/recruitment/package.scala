package com.zarucki

import scala.util.{ Failure, Try }
import scala.util.control.NonFatal

package object recruitment {
  implicit class TryOps[T](underlying: Try[T]) {
    def onNonFatalFailure(action: Throwable => Unit): Try[T] = {
      underlying.recoverWith {
        case NonFatal(t) => action(t)
          Failure(t)
      }
    }
  }
}
