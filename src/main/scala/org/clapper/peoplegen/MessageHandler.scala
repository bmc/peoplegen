package org.clapper.peoplegen

/** Base message handler
  */
trait MessageHandler {
  def verbose(msg: String): Unit
}

/** A message handler that actually emits messages.
  */
object VerboseMessageHandler extends MessageHandler {
  def verbose(msg: String): Unit = println(msg)
}

/** A message handler that suppresses messages.
  */
object EmptyMessageHandler extends MessageHandler {
  def verbose(msg: String): Unit = ()
}
