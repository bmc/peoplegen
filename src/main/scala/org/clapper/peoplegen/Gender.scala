package org.clapper.peoplegen

/** Internal representation of generated gender
  */
object Gender extends Enumeration {
  type Gender = Value

  val Female = Value("F")
  val Male   = Value("M")
}
