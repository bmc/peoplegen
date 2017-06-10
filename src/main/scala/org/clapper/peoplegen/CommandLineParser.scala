package org.clapper.peoplegen

import java.nio.file.{Path, Paths}

import scala.util.{Failure, Success, Try}

private[peoplegen] object FileFormat extends Enumeration {
  type FileFormat = Value

  val CSV = Value
  val JSON = Value
}

private[peoplegen] object HeaderFormat extends Enumeration {
  type HeaderFormat = Value

  val SnakeCase = Value
  val English   = Value
  val CamelCase = Value
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
  * @param yearMin          Minimum birth year
  * @param yearMax          Maximum birth year
  * @param columnSep        Separator to use in CSV mode
  * @param totalPeople      Total people records to generate
  * @param outputFile       Where to write the output (or stdout if None)
  */
private[peoplegen] case class Params(
  fileFormat:      FileFormat = FileFormat.CSV,
  femalePercent:   Int = 50,
  malePercent:     Int = 50,
  generateSSNs:    Boolean = false,
  generateHeader:  Boolean = false,
  headerFormat:    HeaderFormat = HeaderFormat.CamelCase,
  salaryInfo:      Option[SalaryInfo] = None,
  yearMin:         Option[Int] = None,
  yearMax:         Option[Int] = None,
  columnSep:       Char = ',',
  totalPeople:     Int = 0,
  outputFile:      Option[Path] = None
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

  implicit val fileFormatRead: Read[FileFormat.Value] = Read.reads {
    case "csv"  => FileFormat.CSV
    case "json" => FileFormat.JSON
    case s      => throw new CommandLineException(s"""Unknown format: $s""")
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

      opt[Unit]("salary")
        .optional
        .text("Generate salary data. Salaries are generated as a normal " +
              s"distribution with a default mean of ${SalaryInfo.DefaultMean}" +
              "and a default sigma (standard deviation) of " +
              s"${SalaryInfo.DefaultSigma}. To adjust these values, use " +
              s"--salary-mean and --salary-sigma")
        .action { (_, params) =>
          if (params.salaryInfo.nonEmpty)
            params
          else
            params.copy(salaryInfo = Some(SalaryInfo()))
        }

      opt[Int]("salary-mean")
        .optional
        .text("Change the salary generation mean. Note that changing this " +
              "parameter can result in negative salaries, so check your" +
              "final data.")
        .action { case (n, params) =>
          if (params.salaryInfo.isEmpty)
            params.copy(salaryInfo = Some(SalaryInfo(mean = n)))
          else
            params.copy(salaryInfo = params.salaryInfo.map { si =>
              si.copy(mean = n)
            })
        }

      opt[Int]("salary-sigma")
        .optional
        .text("Change the salary generation sigma (i.e., standard deviation). " +
              "Note that changing this parameter can result in negative " +
              "salaries, so check your final data.")
        .action { case (n, params) =>
          if (params.salaryInfo.isEmpty)
            params.copy(salaryInfo = Some(SalaryInfo(sigma = n)))
          else
            params.copy(salaryInfo = params.salaryInfo.map { si =>
              si.copy(sigma = n)
            })
        }

      opt[String]("delim")
        .optional
        .text("""Delimiter to use in CSV mode. Use \t for tab.""")
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
        .text("Whether or not to generate a header for CSV output.")
        .action { (_, params) => params.copy(generateHeader = true) }

      opt[FileFormat.Value]('F', "format")
        .optional
        .text("File format to generate. Allowable values: csv, tsv, json")
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

      opt[Unit]("snake")
        .optional
        .text("Use snake_case for column names.")
        .action { case (_, params) =>
          params.copy(headerFormat = HeaderFormat.SnakeCase)
        }

      arg[Int]("<total>")
        .required
        .text("Total number of names to generate")
        .action  { case (n, params) => params.copy(totalPeople = n) }

      arg[Path]("[<outputfile>]")
        .optional
        .text("Output path.")
        .action { case (path, params) => params.copy(outputFile = Some(path)) }

      checkConfig { params =>
        if ((params.femalePercent + params.malePercent) > 100)
          failure("Female and male percentages add up to more than 100.")
        else
          success
      }

      help("help").text("This usage message.")

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
          val line2 = w.wrap(line.dropWhile(Character.isWhitespace))
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
