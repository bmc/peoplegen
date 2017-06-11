package org.clapper.peoplegen.converters

import java.text.DateFormat

import org.clapper.peoplegen.Fields.FieldSet
import org.clapper.peoplegen.{HeaderFormat, MessageHandler, Person}

import scala.util.{Success, Try}

/** Hides the Person-to-CSV conversion logic, allowing easy substitution
  * of another CSV library.
  *
  * @param headerFormat    header format, used for field name generation
  * @param delimiter       the delimiter to use
  * @param writeHeader     whether or not to write a head
  * @param writeSSNs       whether or not to write SSNs
  * @param writeSalaries   whether or not to write salaries
  * @param dateFormat      a date formatter
  * @param msg             `MessageHandler` to use
  */
class CSVConverter(val headerFormat:  HeaderFormat.Value,
                   delimiter:         Char,
                   writeHeader:       Boolean,
                   writeSSNs:         Boolean,
                   writeSalaries:     Boolean,
                   dateFormat:        DateFormat,
                   msg:               MessageHandler)
  extends Converter {

  private val QuoteChars = Array('"', '\t', ' ', '\n', delimiter)

  import org.clapper.peoplegen.Fields._

  // This is simple output conversion. No CSV library is really needed.

  def header: String = {
    val names = FieldNames(headerFormat)

    FieldSet.inOrder.flatMap {
      case h @ FieldSet.SSN    => if (writeSSNs) Some(names(h)) else None
      case h @ FieldSet.Salary => if (writeSalaries) Some(names(h)) else None
      case h                   => Some(names(h))
    }
    .map(quoteField)
    .mkString(delimiter.toString)
  }

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
  def convertPeople(people: Stream[Person]): Try[Stream[String]] = {
    Try {
      header #:: people.map(convertOne)
    }
  }

  /** Convert a `Person` to a JSON string.
    *
    * @param person  the person
    *
    * @return a `Success` containing the JSON string, or a `Failure` on error
    */
  def convertPerson(person: Person): Try[String] = Success(convertOne(person))

  // --------------------------------------------------------------------------
  // Private Methods
  // --------------------------------------------------------------------------

  private def convertOne(person: Person): String = {
    def personToCSVFields(p: Person): Seq[String] = {
      FieldSet.inOrder.flatMap {
        case FieldSet.SSN =>
          if (writeSSNs) Some(p.ssn) else None
        case FieldSet.Salary =>
          if (writeSalaries) Some(p.salary.toString) else None
        case FieldSet.BirthDate =>
          Some(dateFormat.format(p.birthDate))
        case FieldSet.FirstName =>
          Some(p.firstName)
        case FieldSet.MiddleName =>
          Some(p.middleName)
        case FieldSet.LastName =>
          Some(p.lastName.toString)
        case FieldSet.Gender =>
          Some(p.gender.toString)
      }
    }

    encodeRow(personToCSVFields(person))
  }

  private def quoteField(s: String): String = {
    if (s exists { c => QuoteChars contains c })
      s""""$s""""
    else
      s
  }

  private def encodeRow(row: Seq[String]): String = {
    row.map(quoteField).mkString(delimiter.toString)
  }
}
