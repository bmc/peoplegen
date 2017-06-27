package org.clapper.peoplegen

import java.util.Date

/** The internal form of a generated person.
  *
  * @param id           the ID
  * @param firstName    the first name
  * @param middleName   the middle name
  * @param lastName     the last name
  * @param gender       the gender
  * @param birthDate    the birth date
  * @param salary       the salary, as an integer
  * @param ssn          the (fake) SSN
  */
case class Person(firstName:  String,
                  middleName: String,
                  lastName:   String,
                  gender:     Gender.Value,
                  birthDate:  Date,
                  salary:     Int,
                  ssn:        String,
                  id:         Int)

