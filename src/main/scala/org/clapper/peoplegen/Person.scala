package org.clapper.peoplegen

import java.util.Date

/**
  */
case class Person(firstName:  String,
                  middleName: String,
                  lastName:   String,
                  gender:     String,
                  birthDate:  Date,
                  salary:     Option[Int],
                  ssn:        Option[String])

