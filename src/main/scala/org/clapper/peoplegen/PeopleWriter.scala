package org.clapper.peoplegen

import java.io.{FileWriter, OutputStreamWriter, Writer}
import java.nio.file.Path
import java.text.{DateFormat, SimpleDateFormat}

import org.clapper.peoplegen.converters.{CSVConverter, Converter, JSONConverter}

import scala.util.Try

/** The `PeopleWriter` class must be passed an instance of this class,
  * which controls how the output is opened. This strategy makes it easier
  * to test the writer.
  */
trait OutputHandler {

  /** Opens the output file.
    *
    * @param outputFile The output file to open. If `None`, standard output
    *                   should be used. Testers can override this behavior.
    * @param code       The code to run with the open output stream.
    *
    * @tparam T         The type returned within the `Try` from the code block.
    *
    * @return whatever the code block returns.
    *
    */
  def withOutputFile[T](outputFile: Option[Path])(code: Writer => Try[T]): Try[T]
}

/** Writes a stream of people in the appropriate format. Must be coupled
  * with an OutputHandler. For production use, use the `MainPropleWriter`.
  */
trait PeopleWriter {

  self: OutputHandler =>

  /** the parsed command line parameters
    */
  val params: Params

  /** the message handler to use to emit messages
    */
  val msg: MessageHandler

  /** the format in which to write the birth date
    */
  val BirthDateFormat: DateFormat

  // --------------------------------------------------------------------------
  // Public Methods
  // --------------------------------------------------------------------------

  /** Write a stream of `Person` objects to the output file specified in the
    * parameters passed to the constructor.
    *
    * @param people  the stream of generated people
    *
    * @return `Success` on success, `Failure` on error
    */
  def write(people: LazyList[Person]): Try[Unit] = {
    msg.verbose(s"Writing ${params.totalPeople} people records.")

    def convert(): Try[LazyList[String]] = {
      timeOp(s"converting data to ${params.fileFormat}", msg) {
        converterFor(params.fileFormat).convertPeople(people)
      }
    }

    withOutputFile(params.outputFile) { out =>
      for { data <- convert()
            _    <- write(data, out)
            _    <- Try { out.flush() } }
      yield ()
    }
  }

  // --------------------------------------------------------------------------
  // Private Methods
  // --------------------------------------------------------------------------

  private def converterFor(format: FileFormat.Value): Converter = {
    format match {
      case FileFormat.CSV  =>
        new CSVConverter(
          headerFormat  = params.headerFormat,
          delimiter     = params.columnSep,
          writeIDs      = params.generateIDs,
          writeHeader   = params.generateHeader,
          writeSSNs     = params.generateSSNs,
          writeSalaries = params.generateSalaries,
          dateFormat    = BirthDateFormat,
          msg           = this.msg
        )
      case FileFormat.JSON =>
        new JSONConverter(
          headerFormat  = params.headerFormat,
          writeIDs      = params.generateIDs,
          writeSSNs     = params.generateSSNs,
          writeSalaries = params.generateSalaries,
          dateFormat    = BirthDateFormat,
          jsonFormat    = params.jsonFormat,
          msg           = this.msg
        )
    }
  }

  private def write(strings: LazyList[String], out: Writer): Try[Unit] = {
    timeOp("resolving stream and writing people records to output", msg) {
      Try {
        strings.foreach(s => out.write(s"$s\n"))
      }
    }
  }
}

/** The "production" output handler.
  *
  * @param params the parsed command line parameters
  * @param msg    the message handler to use to emit messages
  */
class MainPeopleWriter(val params: Params, val msg: MessageHandler)
  extends PeopleWriter with OutputHandler {

  val BirthDateFormat = new SimpleDateFormat("yyyy-MM-dd")

  /** Handles writing the output to the actual destinations.
    *
    * @param outputFile The output file to open. If `None`, standard output
    *                   should be used. Testers can override this behavior.
    * @param code       The code to run with the open output stream.
    *
    * @tparam T         The type returned within the `Try` from the code block.
    *
    * @return whatever the code block returns.
    */
  def withOutputFile[T](outputFile: Option[Path])
                       (code: Writer => Try[T]): Try[T] = {
    def getOutput: Try[(Writer, Boolean)] = {
      Try {
        outputFile.map { path =>
          (new FileWriter(path.toFile), true)
        }
        .getOrElse {
          (new OutputStreamWriter(System.out), false)
        }
      }
    }

    def flushAndClose(out: Writer, close: Boolean): Try[Unit] = {
      Try {
        timeOp("flushing output stream", msg) {
          out.flush()
          if (close) out.close()
        }
      }
    }

    for { (out, close)   <- getOutput
          res <- code(out)
          _ <- flushAndClose(out, close) }
    yield res
  }
}
