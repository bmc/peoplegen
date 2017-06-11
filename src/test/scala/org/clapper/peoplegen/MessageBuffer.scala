package org.clapper.peoplegen

import java.io.{ByteArrayOutputStream, OutputStreamWriter, PrintStream}

/**
  */
class MessageBuffer {
  val buffer = new ByteArrayOutputStream

  val asPrintStream = new PrintStream(buffer)
  val asWriter = new OutputStreamWriter(buffer)

  def asString: String = buffer.toString(defaultEncoding)

  private def defaultEncoding = {
    Option(System.getProperty("file.encoding")).getOrElse("UTF-8")
  }

}
