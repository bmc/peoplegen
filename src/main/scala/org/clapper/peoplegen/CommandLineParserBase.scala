package org.clapper.peoplegen

import java.io.PrintStream
import java.nio.file.{Path, Paths}

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/** File format to generate.
  */
private[peoplegen] object FileFormat extends Enumeration {
  type FileFormat = Value

  val CSV = Value("csv")
  val JSON = Value("json")
}

/** Header format to use.
  */
private[peoplegen] object HeaderFormat extends Enumeration {
  type HeaderFormat = Value

  val SnakeCase = Value
  val English   = Value
  val CamelCase = Value
}

/** How to generate the JSON output, if JSON is being generated.
  */
private[peoplegen] object JSONFormat extends Enumeration {
  type JSONFormat = Value

  /** Generate the people in one big JSON array of object.
    */
  val CompactArray = Value("array")

  /** Generate the people as one JSON object per line, not wrapped in an
    * array. (Apache Spark wants this format, for instance.)
    */
  val CompactRows = Value("rows")

  /** Pretty-print the output.
    */
  val Pretty = Value("pretty")
}

case class SalaryInfo(mean:  Int = SalaryInfo.DefaultMean,
                      sigma: Int = SalaryInfo.DefaultSigma)
object SalaryInfo {
  val DefaultMean = 72641
  val DefaultSigma = 20000
}

import FileFormat.FileFormat
import HeaderFormat.HeaderFormat

/** The parsed command line parameters
  *
  * @param fileFormat       Desired file format
  * @param femalePercent    Percentage of female names
  * @param malePercent      Percentage of male names
  * @param generateSSNs     Whether or not to generate Social Security Numbers
  * @param generateHeader   Whether or not to generate a header (CSV only)
  * @param generateIDs      Whether or not to generate unique per-row ID values
  * @param headerFormat     The format of the file header (CSV only)
  * @param salaryInfo       Salary generation data
  * @param generateSalaries Whether or not to generate salary info
  * @param yearStart          Minimum birth year
  * @param yearEnd          Maximum birth year
  * @param columnSep        Separator to use in CSV mode
  * @param jsonFormat       how to generate JSON
  * @param verbose          Whether or not to generate verbose messages
  * @param totalPeople      Total people records to generate
  * @param outputFile       Where to write the output (or stdout if None)
  */
private[peoplegen] case class Params(
  fileFormat:       FileFormat = FileFormat.CSV,
  femalePercent:    Int = -1,
  malePercent:      Int = -1,
  generateSSNs:     Boolean = false,
  generateHeader:   Boolean = false,
  generateIDs:      Boolean = false,
  headerFormat:     HeaderFormat = HeaderFormat.CamelCase,
  salaryInfo:       SalaryInfo = SalaryInfo(),
  generateSalaries: Boolean = false,
  yearStart:        Option[Int] = None,
  yearEnd:          Option[Int] = None,
  columnSep:        Char = ',',
  jsonFormat:       JSONFormat.Value = JSONFormat.CompactRows,
  verbose:          Boolean = false,
  totalPeople:      Int = 0,
  outputFile:       Option[Path] = None
) {


  val startingYear = yearStart.getOrElse(
    Params.ThisYear - Params.StartingYearDefaultDelta
  )
  val endingYear = yearEnd.getOrElse(
    Params.ThisYear - Params.EndingYearDefaultDelta
  )
}

object Params {
  val StartingYearDefaultDelta = 65
  val EndingYearDefaultDelta   = 18

  private val ThisYear = {
    import java.util.Calendar

    val cal = Calendar.getInstance
    cal.get(Calendar.YEAR)
  }
}

/** Thrown to indicate a problem with the command line.
  *
  * @param message the message, if any
  */
private[peoplegen] class CommandLineException(val message: String = "")
  extends Exception(message)

/** Thrown to indicate the code *outside* scopt should display the usage
  * message.
  */
private[peoplegen] class UsageException(message: String = "")
  extends CommandLineException(message)

/** A trait that implements that command line parser. Use the
  * `CommandLineParser` for actual production parsing. Implement this
  * trait yourself for testing.
  */
private[peoplegen] trait CommandLineParserBase {
  import scopt._

  private implicit val pathRead: Read[Path] = Read.reads { s => Paths.get(s) }

  private implicit val fileFormatRead: Read[FileFormat.Value] = Read.reads { s =>
    Try {
      FileFormat.withName(s)
    }
    .recoverWith {
      case NonFatal(e) =>
        Failure(new CommandLineException(s"""Unknown file format: $s"""))
    }
    .get
  }

  private implicit val jsonFormatRead: Read[JSONFormat.Value] = Read.reads { s =>
    Try {
      JSONFormat.withName(s)
    }
    .recoverWith {
      case NonFatal(e) =>
        Failure(new CommandLineException(s"""Unknown JSON format: $s"""))
    }
    .get
  }

  /** Parse the command line parameters.
    *
    * @param args      command line arguments
    *
    * @return `Success(Params)` on success. On error, error messages will have
    *         been written to the console, and this method will return
    *         `Failure(exception)`, with an exception you can ignore.
    */
  def parseParams(args: Array[String]): Try[Params] = {
    val parser = getParser()
    Console.withOut(outputStream) {
      Console.withErr(errorStream) {
        val t = parser
          .parse(args, Params())
          .map(postCheck)
          .getOrElse(Failure(new CommandLineException()))

        t match {
          case t @ Success(_) =>
            t
          case Failure(e: UsageException) =>
            val msg = e.message
            if (msg.nonEmpty) errorStream.println(s"Error: $msg")
            Failure(e)

          case Failure(e: CommandLineException) =>
            val msg = e.message
            if (msg.nonEmpty) errorStream.println(s"Error: $msg")
            Failure(e)

          case f @ Failure(e) =>
            f
        }
      }
    }
  }

  // --------------------------------------------------------------------------
  // Protected Methods
  // --------------------------------------------------------------------------

  /** Defines the standard output (i.e., where the parser should send text
    * written to stdout).
    *
    * @return the output stream
    */
  protected def outputStream: PrintStream

  /** Defines the standard error (i.e., where the parser should send text
    * written to stderr).
    *
    * @return the error stream
    */
  protected def errorStream: PrintStream

  // --------------------------------------------------------------------------
  // Private Methods
  // --------------------------------------------------------------------------

  /** Do post-checks on the parsed parameters.
    *
    * @param params the parameters, as parsed
    */
  private def postCheck(params: Params): Try[Params] = {
    // Have to do the checkConfig-type work here, because scopt's
    // checkConfig doesn't allow updating the params.

    def checkPercents(params: Params): Try[Params] = {
      // If no female or male percentage were specified, then both will
      // be -1, so we can set them to 50:50. Otherwise, if one of them is
      // -1, set it to 100 minus the other one. If both are set, though,
      // verify that they add up to 100.
      (params.femalePercent, params.malePercent) match {
        case (f, m) if (m < 0) && (f < 0) =>
          Success(params.copy(femalePercent = 50, malePercent = 50))

        case (f, m) if m < 0 =>
          Success(params.copy(malePercent = 100 - f))

        case (f, m) if f < 0 =>
          Success(params.copy(femalePercent = 100 - m))

        case (f, m) if (m + f) != 100 =>
          Failure(new UsageException(
            "Female and male percentages do not add up to 100."
          ))

        case _ =>
          Success(params)
      }
    }

    def checkYears(params: Params): Try[Params] = {
      if (params.startingYear > params.endingYear)
        Failure(new UsageException(
          s"Maximum year ${params.endingYear} is less than minimum " +
          s"year ${params.startingYear}"
        ))
      else
        Success(params)
    }

    for { p1 <- checkPercents(params)
          p2 <- checkYears(p1) }
      yield p2
  }

  /** Get the scopt parser object.
    *
    * @param buildInfo the build information file
    *
    * @return the parser
    */
  private def getParser(): OptionParser[Params] = {

    new scopt.OptionParser[Params](Main.Constants.Name) {
      override val showUsageOnError = Option(true)
      override def renderingMode = RenderingMode.OneColumn

      head(s"\n${BuildInfo.toString}\n")

      help("help").text("This usage message.")

      opt[Int]('f', "female")
        .optional
        .valueName("<percent>")
        .text("Percentage of female names. Defaults to 50.")
        .action { (percent, params) => params.copy(femalePercent = percent) }

      opt[Int]('m', "male")
        .optional
        .valueName("<percent>")
        .text("Percentage of male names. Defaults to 50.")
        .action { (percent, params) => params.copy(malePercent = percent) }

      opt[Unit]("id")
        .optional
        .text("Generate unique per-row IDs.")
        .action { (_, params) => params.copy(generateIDs = true) }

      opt[Unit]("ssn")
        .optional
        .text("Generate (fake) Social Security Numbers.")
        .action { (_, params) => params.copy(generateSSNs = true) }

      opt[Unit]("salary")
        .optional
        .text("Generate salary data. Salaries are generated as a normal " +
              s"distribution around a mean of ${SalaryInfo.DefaultMean} " +
              "(the U.S. mean salary in 2014), with a sigma (spread) of " +
              s"${SalaryInfo.DefaultSigma}. To changes these values, use " +
              "--salary-mean and --salary-sigma.")
        .action { (_, params) => params.copy(generateSalaries = true) }

      opt[Int]("salary-mean")
        .optional
        .text("Change the salary generation mean. Note: Changing this value " +
              "can result in negative salaries, so check your final data.")
        .action { case (n, params) =>
          params.copy(salaryInfo = params.salaryInfo.copy(mean = n))
        }

      opt[Int]("salary-sigma")
        .optional
        .text("Change the salary generation sigma (i.e., standard deviation). " +
              "Note: Changing this value can result in negative salaries, " +
              "so check your final data.")
        .action { case (n, params) =>
          params.copy(salaryInfo = params.salaryInfo.copy(sigma = n))
        }

      opt[Int]("year-min")
        .optional
        .text("Specify the starting year for birth dates. Defaults to " +
              s"${Params.StartingYearDefaultDelta} years ago from this year.")
        .action { case (n, params) => params.copy(yearStart = Some(n)) }

      opt[Int]("year-max")
        .optional
        .text("Specify the ending year for birth dates. Defaults to " +
          s"${Params.EndingYearDefaultDelta} years ago from this year.")
        .action { case (n, params) => params.copy(yearEnd = Some(n)) }

      opt[String]("delim")
        .optional
        .valueName("<c>")
        .text("""Delimiter to use in CSV mode. Use "\t" for tab.""")
        .validate {
          case """\t""" => success
          case s if s.length == 1 => success
          case _ => failure("""--delim must be a single character or \t""")
        }
        .action { (s, params) =>
          val delim = if (s == """\t""") "\t" else s
          params.copy(columnSep = delim.head)
        }

      opt[Unit]("header")
        .optional
        .text("Generate a header for CSV output. Default: no header")
        .action { (_, params) => params.copy(generateHeader = true) }

      opt[FileFormat.Value]('F', "format")
        .valueName("<format>")
        .optional
        .text("File format to generate. Allowable values: csv, json")
        .action { case (fmt, params)  => params.copy(fileFormat = fmt) }

      opt[Unit]("camel")
        .optional
        .text("Use camelCase for column names.")
        .action { case (_, params) =>
          params.copy(headerFormat = HeaderFormat.CamelCase)
        }

      opt[Unit]("english")
        .optional
        .text("Use English (space-separated) names for column names.")
        .action { case (_, params) =>
          params.copy(headerFormat = HeaderFormat.English)
        }

      opt[JSONFormat.Value]('j', "json-format")
        .optional
        .valueName("<format>")
        .text("If generating JSON, specify how the JSON is generated.\\n" +
              s""""${JSONFormat.CompactArray}": write one JSON array of records.\\n""" +
              s""""${JSONFormat.CompactRows}": write one JSON object per line.\\n""" +
              s""""${JSONFormat.Pretty}": pretty-print the JSON.\\n""" +
              s"Default: ${JSONFormat.CompactRows}")
        .action { case (fmt, params) => params.copy(jsonFormat = fmt) }

      opt[Unit]("snake")
        .optional
        .text("Use snake_case for column names.")
        .action { case (_, params) =>
          params.copy(headerFormat = HeaderFormat.SnakeCase)
        }

      opt[Unit]('v', "verbose")
        .optional
        .text("Emit (some) verbose messages")
        .action { case (_, params) => params.copy(verbose = true) }

      arg[Int]("<total>")
        .required
        .text("Total number of names to generate")
        .action  { case (n, params) => params.copy(totalPeople = n) }

      arg[Path]("<outputfile>")
        .optional
        .text("Output path.")
        .action { case (path, params) => params.copy(outputFile = Some(path)) }
    }
  }
}

/** The "production" command line parser.
  */
object CommandLineParser extends CommandLineParserBase {
  /** Defines the standard output (i.e., where the parser should send text
    * written to stdout).
    *
    * @return the output stream
    */
  protected def outputStream: PrintStream = System.out

  /** Defines the standard error (i.e., where the parser should send text
    * written to stderr).
    *
    * @return the error stream
    */
  protected def errorStream: PrintStream = System.err
}
