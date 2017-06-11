package org.clapper.peoplegen

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
  val AsArray = Value("array")

  /** Generate the people as one JSON object per line, not wrapped in an
    * array. (Apache Spark wants this format, for instance.)
    */
  val AsRows = Value("rows")
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
  * @param headerFormat     The format of the file header (CSV only)
  * @param salaryInfo       Salary generation data
  * @param generateSalaries Whether or not to generate salary info
  * @param yearMin          Minimum birth year
  * @param yearMax          Maximum birth year
  * @param columnSep        Separator to use in CSV mode
  * @param prettyJSON       Pretty-print JSON, instead of printing it in
  *                         one compact line.
  * @param jsonFormat       how to generate JSON
  * @param verbose          Whether or not to generate verbose messages
  * @param totalPeople      Total people records to generate
  * @param outputFile       Where to write the output (or stdout if None)
  */
private[peoplegen] case class Params(
  fileFormat:       FileFormat = FileFormat.CSV,
  femalePercent:    Int = 50,
  malePercent:      Int = 50,
  generateSSNs:     Boolean = false,
  generateHeader:   Boolean = false,
  headerFormat:     HeaderFormat = HeaderFormat.CamelCase,
  salaryInfo:       SalaryInfo = SalaryInfo(),
  generateSalaries: Boolean = false,
  yearMin:          Option[Int] = None,
  yearMax:          Option[Int] = None,
  columnSep:        Char = ',',
  prettyJSON:       Boolean = false,
  jsonFormat:       JSONFormat.Value = JSONFormat.AsRows,
  verbose:          Boolean = false,
  totalPeople:      Int = 0,
  outputFile:       Option[Path] = None
) {

  private val thisYear = {
    import java.util.Calendar

    val cal = Calendar.getInstance
    cal.get(Calendar.YEAR)
  }

  val minimumYear = yearMin.getOrElse(thisYear - 65)
  val maximumYear = yearMax.getOrElse(thisYear - 18)
}

/** Thrown to indicate a problem with the command line.
  *
  * @param message the message, if any
  */
private[peoplegen] class CommandLineException(val message: String = "")
  extends Exception(message)

/**
  */
private[peoplegen] trait CommandLineParser {
  import scopt._

  implicit val pathRead: Read[Path] = Read.reads { s => Paths.get(s) }

  implicit val fileFormatRead: Read[FileFormat.Value] = Read.reads { s =>
    Try {
      FileFormat.withName(s)
    }
    .recoverWith {
      case NonFatal(e) =>
        Failure(new CommandLineException(s"""Unknown file format: $s"""))
    }
    .get
  }

  implicit val jsonFormatRead: Read[JSONFormat.Value] = Read.reads { s =>
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
    * @param buildInfo the build information file
    *
    * @return `Success(Params)` on success. On error, error messages will have
    *         been written to the console, and this method will return
    *         `Failure(exception)`, with an exception you can ignore.
    */
  def parseParams(args: Array[String], buildInfo: BuildInfo): Try[Params] = {
    getParser(buildInfo).parse(args, Params())
      .map(Success(_))
      .getOrElse(Failure(new CommandLineException()))
  }

  /** Get the scopt parser object.
    *
    * @param buildInfo the build information file
    *
    * @return the parser
    */
  private def getParser(buildInfo: BuildInfo): OptionParser[Params] = {

    new scopt.OptionParser[Params](Main.Constants.Name) {
      override val showUsageOnError = true
      override def renderingMode = RenderingMode.OneColumn

      head(s"\n${buildInfo.toString}\n")

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

      opt[Unit]("ssn")
        .optional
        .text("Whether or not to generate (fake) SSNs.")
        .action { (_, params) => params.copy(generateSSNs = true) }

      opt[Unit]("salaries")
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
        .text("If generating JSON, specify how the JSON is generated. " +
              "Ignored if --pretty is specified. Legal values:\\n" +
              s""""${JSONFormat.AsArray}": write one JSON array of records.\\n""" +
              s""""${JSONFormat.AsRows}": write one JSON object per line.\\n""" +
              s"Default: ${JSONFormat.AsRows}")
        .action { case (fmt, params) => params.copy(jsonFormat = fmt) }

      opt[Unit]("pretty")
        .optional
        .text("Pretty-print JSON, instead of printing it all on one line. " +
              """honored if --format is "json".""")
        .action { case (_, params) =>
          params.copy(prettyJSON = true)
        }

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

      checkConfig { params =>

        if ((params.femalePercent + params.malePercent) > 100)
          failure("Female and male percentages add up to more than 100.")
        else
          success
      }

      override def showUsage(): Unit = {
        import grizzled.string.WordWrapper

        // scopt's generated usage message doesn't wrap properly. Add wrapping.
        // Since we don't have access to all of scopt's internals, we have
        // to brute-force this one.
        val cols = Option(System.getenv("COLUMNS")).flatMap { sColumns =>
          Try { Some(sColumns.toInt) }.recover { case _: Exception => None }.get
        }
        .getOrElse(80) - 1

        val lines = usage.split("\n")
        val u = for (line <- lines) yield {
          val leadingBlanks = line.takeWhile(Character.isWhitespace).length
          val w = WordWrapper(indentation = leadingBlanks, wrapWidth = cols)
          // Special case: Allow the metacharacter string \n as an embedded
          // newline token.
          val line2 = w.wrap(line.dropWhile(Character.isWhitespace).replace("\\n", "\n"))
          if (line2 startsWith "Command")
            s"\n$line2"
          else
            line2
        }
        Console.err.println(u.mkString("\n"))
      }

      override def showUsageAsError: Unit = showUsage()
    }
  }
}
