package org.clapper.peoplegen

import java.util.Date

/**
  */
case class Person(firstName:  String,
                  middleName: String,
                  lastName:   String,
                  gender:     Gender.Value,
                  birthDate:  Date,
                  salary:     Int,
                  ssn:        String)

