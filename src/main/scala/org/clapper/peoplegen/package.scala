package org.clapper

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

/** Top-level functions.
  */
package object peoplegen {

  /** Time an operation, displaying a message if verbosity is enabled.
    *
    * @param  msg   the message handler
    * @param  label operation label, fro printing
    * @param  op    the code to time
    * @tparam T     type of the return value of `code`
    *
    * @return whatever `code` returns
    */
  def timeOp[T](label: String, msg: MessageHandler)(op: => T): T = {
    import grizzled.datetime.Implicits.EnrichedDuration

    val start = System.currentTimeMillis
    msg.verbose(s"Started $label.")
    try {
      op
    }
    finally {
      val end = System.currentTimeMillis
      val duration = Duration(end - start, TimeUnit.MILLISECONDS)
      msg.verbose(s"Finished $label in ${duration.humanize}.")
    }
  }
}
