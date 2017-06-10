package org.clapper.peoplegen

import scala.util.{Failure, Success, Try}

object Main {

  object Constants {
    val Name = "peoplegen"
  }

  def main(args: Array[String]): Unit = {

    val parser = new CommandLineParser {}
    val t = for { bi        <- BuildInfo.load()
                  params    <- parser.parseParams(args, bi)
                  generator  = new PeopleGenerator(params)
                  people    <- generator.generatePeople
                  writer     = new PeopleWriter(params)
                  _         <- writer.write(people) }
      yield ()

    val rc = t match {
      case Success(params) =>
        0
      case Failure(e: CommandLineException) =>
        println(e.message)
        1
      case Failure(e) =>
        e.printStackTrace(System.err)
        1
    }

    System.exit(rc)
  }
}
