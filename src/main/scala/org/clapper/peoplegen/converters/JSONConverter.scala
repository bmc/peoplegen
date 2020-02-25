package org.clapper.peoplegen.converters

import java.text.DateFormat

import org.clapper.peoplegen.Fields.{FieldNames, FieldSet}
import org.clapper.peoplegen._
import spray.json._

import scala.util.Try


/** Spray JSON conversion protocol (write-only, no read).
  *
  * @param headerFormat    header format, used for field name generation
  * @param writeIDs        whether or not to write IDs
  * @param writeSSNs       whether or not to write SSNs
  * @param writeSalaries   whether or not to write salaries
  * @param dateFormat      a date formatter
  */
private[peoplegen] class PersonProtocol(headerFormat:  HeaderFormat.Value,
                                        writeIDs:      Boolean,
                                        writeSSNs:     Boolean,
                                        writeSalaries: Boolean,
                                        dateFormat:    DateFormat)
  extends DefaultJsonProtocol {

  implicit object PersonJsonFormat extends RootJsonFormat[Person] {
    def write(p: Person): JsValue = {
      val names = FieldNames(headerFormat)
      val fields = FieldSet.inOrder.flatMap {
        case h @ FieldSet.ID =>
          if (writeIDs) Some(names(h) -> JsNumber(p.id)) else None
        case h @ FieldSet.SSN =>
          if (writeSSNs) Some(names(h) -> JsString(p.ssn)) else None
        case h @ FieldSet.Salary =>
          if (writeSalaries) Some(names(h) -> JsNumber(p.salary)) else None
        case h @ FieldSet.Gender =>
          Some(names(h) -> JsString(p.gender.toString))
        case h @ FieldSet.FirstName =>
          Some(names(h) -> JsString(p.firstName))
        case h @ FieldSet.MiddleName =>
          Some(names(h) -> JsString(p.middleName))
        case h @ FieldSet.LastName =>
          Some(names(h) -> JsString(p.lastName))
        case h @ FieldSet.BirthDate =>
          Some(names(h) -> JsString(dateFormat.format(p.birthDate)))
      }

      JsObject(fields: _*)
    }

    def read(value: JsValue): Person = {
      deserializationError("Read not supported")
    }
  }
}

/** Hides the Person-to-JSON conversion logic, allowing easy substitution
  * of another JSON library.
  *
  * @param headerFormat    header format, used for field name generation
  * @param writeIDs        whether or not to write IDs
  * @param writeSSNs       whether or not to write SSNs
  * @param writeSalaries   whether or not to write salaries
  * @param dateFormat      a date formatter
  * @param jsonFormat      the JSON format to use. Ignored if `pretty` is `true`.
  * @param msg             `MessageHandler` to use
  */
class JSONConverter(val headerFormat:  HeaderFormat.Value,
                    writeIDs:          Boolean,
                    writeSSNs:         Boolean,
                    writeSalaries:     Boolean,
                    dateFormat:        DateFormat,
                    jsonFormat:        JSONFormat.Value,
                    msg:               MessageHandler)
  extends Converter {

  private val protocol = new PersonProtocol(
    headerFormat  = headerFormat,
    writeIDs      = writeIDs,
    writeSSNs     = writeSSNs,
    writeSalaries = writeSalaries,
    dateFormat    = dateFormat
  )

  import protocol._

  /** Convert a stream of `Person` objects to JSON.
    *
    * @param people  `Stream` of people to convert
    *
    * @return a `Success` containing a `Stream` of one or more JSON strings,
    *         or a `Failure` on error. The stream may contain a single JSON
    *         object (e.g., a JSON array), or it may contain multiple JSON
    *         objects, depending on the `jsonFormat` and `pretty` parameters.
    *
    */
  def convertPeople(people: LazyList[Person]): Try[LazyList[String]] = {

    def convertToJSONArray = {
      msg.verbose(s"Converting people record stream to JSON.")
      people.toVector.toJson
    }

    Try {
      jsonFormat match {
        case JSONFormat.Pretty       => LazyList(convertToJSONArray.prettyPrint)
        case JSONFormat.CompactArray => LazyList(convertToJSONArray.compactPrint)
        case JSONFormat.CompactRows  => people.map(_.toJson.compactPrint)
      }
    }
  }

  /** Convert a `Person` to a JSON string.
    *
    * @param person  the person
    *
    * @return a `Success` containing the JSON string, or a `Failure` on error
    */
  def convertPerson(person: Person): Try[String] = {
    Try {
      jsonFormat match {
        case JSONFormat.Pretty => person.toJson.prettyPrint
        case _                 => person.toJson.compactPrint
      }
    }
  }
}
