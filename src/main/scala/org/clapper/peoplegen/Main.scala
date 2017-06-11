package org.clapper.peoplegen

import scala.util.{Failure, Success, Try}

object Main {

  object Constants {
    val Name = "peoplegen"
  }

  def main(args: Array[String]): Unit = {

    def getMessageHandler(params: Params) = if (params.verbose)
      VerboseMessageHandler
    else
      EmptyMessageHandler

    val parser = new CommandLineParser {}
    val t = for { bi        <- BuildInfo.load()
                  params    <- parser.parseParams(args, bi)
                  msg       =  getMessageHandler(params)
                  generator  = new PeopleGenerator(params, msg)
                  people    <- generator.generatePeople
                  writer     = new PeopleWriter(params, msg)
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
