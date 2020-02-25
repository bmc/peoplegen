package org.clapper.peoplegen.converters

import org.clapper.peoplegen.{HeaderFormat, Person}

import scala.util.Try

/** Base class for a converter.
  */
trait Converter {

  /** The header format to use.
    */
  val headerFormat: HeaderFormat.Value

  /** Convert a stream of people to a stream of output strings.
    *
    * @param people `Stream` of people to convert
    *
    * @return a `Success` containing a `Stream` of one or more strings,
    *         or a `Failure` on error. The stream may contain a single string
    *         for some formats (and with some parameters), or it may contain
    *         multiple strings.
    */
  def convertPeople(people: LazyList[Person]): Try[LazyList[String]]

  /** Convert a single `Person` record to a string.
    *
    * @param person the `Person` record
    *
    * @return a `Success` containing the resulting string, or a `Failure` on
    *         error
    */
  def convertPerson(person: Person): Try[String]
}
