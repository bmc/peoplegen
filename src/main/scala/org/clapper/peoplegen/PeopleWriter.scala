package org.clapper.peoplegen

import java.io.{FileWriter, OutputStreamWriter, Writer}
import java.text.SimpleDateFormat

import scala.util.Try

/** Writes a stream of people in the appropriate format.
  *
  * @param params  the parsed command line parameters
  * @param msg     the message handler to use to emit messages
  */
class PeopleWriter(params: Params, msg: MessageHandler) {

  private val BirthDateFormat        = new SimpleDateFormat("yyyy-MM-dd")
  private val VerbosePersonThreshold = 1000

  private object Headers extends Enumeration {
    type Headers = Value

    val FirstName  = Value
    val MiddleName = Value
    val LastName   = Value
    val BirthDate  = Value
    val SSN        = Value
    val Gender     = Value
    val Salary     = Value

    def inOrder = Seq(
      FirstName, MiddleName, LastName, Gender, BirthDate, SSN, Salary
    )
  }

  private val HeaderNames = Map(
    HeaderFormat.English -> Map(
      Headers.FirstName  -> "first name",
      Headers.MiddleName -> "middle name",
      Headers.LastName   -> "last name",
      Headers.BirthDate  -> "birth date",
      Headers.SSN        -> "ssn",
      Headers.Gender     -> "gender",
      Headers.Salary     -> "salary"
    ),
    HeaderFormat.CamelCase -> Map(
      Headers.FirstName  -> "firstName",
      Headers.MiddleName -> "middleName",
      Headers.LastName   -> "lastName",
      Headers.BirthDate  -> "birthDate",
      Headers.SSN        -> "ssn",
      Headers.Gender     -> "gender",
      Headers.Salary     -> "salary"
    ),
    HeaderFormat.SnakeCase -> Map(
      Headers.FirstName  -> "first_name",
      Headers.MiddleName -> "middle_name",
      Headers.LastName   -> "last_name",
      Headers.BirthDate  -> "birth_date",
      Headers.SSN        -> "ssn",
      Headers.Gender     -> "gender",
      Headers.Salary     -> "salary"
    )
  )

  def write(people: Stream[Person]): Try[Unit] = {
    def dispatch(w: Writer): Try[Unit] = {
      params.fileFormat match {
        case FileFormat.CSV  => writeCSV(people, w)
        case FileFormat.JSON => writeJSON(people, w)
      }
    }

    msg.verbose(s"Writing ${params.totalPeople} people records.")

    params.outputFile.map { path =>
      import grizzled.util.withResource
      import grizzled.util.CanReleaseResource.Implicits.CanReleaseCloseable

      withResource(new FileWriter(path.toFile)) { w =>
        dispatch(w)
      }
    }
    .getOrElse {
      for { f <- Try { new OutputStreamWriter(System.out) }
            _ <- dispatch(f) }
      yield Try {
        f.flush()
      }
    }
  }

  private def atVerboseThreshold(index: Int): Boolean = {
    ((index + 1) % VerbosePersonThreshold) == 0
  }

  private def writeCSV(people: Stream[Person], out: Writer): Try[Unit] = {
    import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}

    def getHeaders: List[String] = {
      Headers.inOrder.map { h => HeaderNames(params.headerFormat)(h) }.toList
    }

    def personToCSVFields(p: Person): List[String] = {
      Headers.inOrder.flatMap {
        case Headers.SSN =>
          if (params.generateSSNs) Some(p.ssn) else None
        case Headers.Salary =>
          if (params.generateSalaries) Some(p.salary.toString) else None
        case Headers.BirthDate =>
          Some(BirthDateFormat.format(p.birthDate))
        case Headers.FirstName =>
          Some(p.firstName)
        case Headers.MiddleName =>
          Some(p.middleName)
        case Headers.LastName =>
          Some(p.lastName.toString)
        case Headers.Gender =>
          Some(p.gender.toString)
      }
      .toList
    }

    Try {
      implicit object MyFormat extends DefaultCSVFormat {
        override val delimiter = params.columnSep
      }

      val w = CSVWriter.open(out)
      if (params.generateHeader)
        w.writeRow(getHeaders)

      for ((p, i) <- people.zipWithIndex) {
        if (atVerboseThreshold(i)) msg.verbose(s"... ${i + 1}")
        w.writeRow(personToCSVFields(p))
      }
    }
  }

  private def writeJSON(people: Stream[Person], out: Writer): Try[Unit] = {
    import spray.json._

    object PersonProtocol extends DefaultJsonProtocol {
      implicit object PersonJsonFormat extends RootJsonFormat[Person] {
        def write(p: Person): JsValue = {
          val names = HeaderNames(params.headerFormat)
          val fields = Headers.inOrder.flatMap {
            case h @ Headers.SSN =>
              if (params.generateSSNs)
                Some(names(h) -> JsString(p.ssn))
              else
                None
            case h @ Headers.Salary =>
              if (params.generateSalaries)
                Some(names(h) -> JsNumber(p.salary))
              else
                None
            case h @ Headers.Gender =>
              Some(names(h) -> JsString(p.gender.toString))
            case h @ Headers.FirstName =>
              Some(names(h) -> JsString(p.firstName))
            case h @ Headers.MiddleName =>
              Some(names(h) -> JsString(p.middleName))
            case h @ Headers.LastName =>
              Some(names(h) -> JsString(p.lastName))
            case h @ Headers.BirthDate =>
              Some(names(h) -> JsString(BirthDateFormat.format(p.birthDate)))
          }

          JsObject(fields: _*)
        }

        def read(value: JsValue): Person = {
          deserializationError("Read not supported")
        }
      }
    }

    Try {
      import PersonProtocol._
      msg.verbose(s"Converting ${params.totalPeople} people records to JSON.")
      val json = people.toSeq.toJson

      val jsonString = if (params.prettyJSON)
        json.prettyPrint
      else
        json.compactPrint

      msg.verbose(s"Writing JSON.")
      out.write(s"$jsonString\n")
    }
  }
}

