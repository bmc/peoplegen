package org.clapper.peoplegen

import java.io.{FileWriter, OutputStreamWriter, Writer}
import java.text.SimpleDateFormat

import scala.util.Try

/**
  */
class PeopleWriter(params: Params) {

  private val BirthDateFormat = new SimpleDateFormat("yyyy-MM-dd")

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

  def write(people: Seq[Person]): Try[Unit] = {
    def dispatch(w: Writer): Try[Unit] = {
      params.fileFormat match {
        case FileFormat.CSV  => writeCSV(people, w)
        case FileFormat.JSON => writeJSON(people, w)
      }

    }
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

  private def writeCSV(people: Seq[Person], out: Writer): Try[Unit] = {
    import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}

    def getHeaders: List[String] = {
      Headers.inOrder.flatMap { h =>
        h match {
          case Headers.SSN => if (params.generateSSNs) Some(h) else None
          case Headers.Salary => params.salaryInfo.map(_ => h)
          case _ => Some(h)
        }
      }
      .map { h =>
        HeaderNames(params.headerFormat)(h)
      }
      .toList
    }

    def personToCSVFields(p: Person): List[String] = {
      Headers.inOrder.flatMap { h =>
        h match {
          case Headers.SSN        => p.ssn
          case Headers.Salary     => p.salary.map(_.toString)
          case Headers.BirthDate  => Some(BirthDateFormat.format(p.birthDate))
          case Headers.FirstName  => Some(p.firstName)
          case Headers.MiddleName => Some(p.middleName)
          case Headers.LastName   => Some(p.lastName)
          case Headers.Gender     => Some(p.gender)
        }
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

      for (p <- people) {
        w.writeRow(personToCSVFields(p))
      }
    }
  }


  private def writeJSON(people: Seq[Person], out: Writer): Try[Unit] = {
    import spray.json._

    object PersonProtocol extends DefaultJsonProtocol {
      implicit object PersonJsonFormat extends RootJsonFormat[Person] {
        def write(p: Person): JsValue = {
          val names = HeaderNames(params.headerFormat)
          val fields = Headers.inOrder.flatMap { h =>
            h match {
              case Headers.SSN =>
                p.ssn.map { num =>
                  names(h) -> JsString(num)
                }
              case Headers.Salary =>
                p.salary.map { num =>
                  names(h) -> JsNumber(num)
                }
              case Headers.Gender =>
                Some(names(h) -> JsString(p.gender))
              case Headers.FirstName =>
                Some(names(h) -> JsString(p.firstName))
              case Headers.MiddleName =>
                Some(names(h) -> JsString(p.middleName))
              case Headers.LastName =>
                Some(names(h) -> JsString(p.lastName))
              case Headers.BirthDate =>
                Some(names(h) -> JsString(BirthDateFormat.format(p.birthDate)))
            }
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

      out.write(people.toJson.compactPrint + "\n")
    }
  }
}
