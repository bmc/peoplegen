package org.clapper.peoplegen

import java.io.IOException

import breeze.stats.distributions.Gaussian
import grizzled.random.RandomUtil
import java.util.{Calendar, Date, TimeZone}

import scala.io.Source
import scala.util.{Failure, Try}

/** Generates the people. The generator generates all fields; it's up to the
  * output routines to decide what fields to write.
  *
  * @param params  the parsed command line parameters
  * @param msg     the message handler to use to emit messages
  */
class PeopleGenerator(params: Params, msg: MessageHandler) {

  private val classLoader  = this.getClass.getClassLoader
  private val SSNPrefixes  = (900 to 999).toArray :+ 666
  private val today        = new Date
  private val todayCal     = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
  private val minBirthDate = makeDate(params.startingYear, 0, 1)
  private val maxBirthDate = makeDate(params.endingYear, 12, 31)
  private val minEpoch     = minBirthDate.getTime
  private val maxEpoch     = maxBirthDate.getTime

  // To generate the salaries we use a Gaussian generator, which generates
  // a normal (bell-curve) distribution around the mean.
  private val salaryGen    =  Gaussian(mu    = params.salaryInfo.mean,
                                       sigma = params.salaryInfo.sigma)

  /** Generate `params.totalPeople` people records.
    *
    * @return a `Success` containing a lazy stream of the people, or a
    *         `Failure` on error
    */
  def generatePeople: Try[Stream[Person]] = {

    for {lastNames        <- loadNames("last_names.txt")
         maleFirstNames   <- loadNames("male_first_names.txt")
         femaleFirstNames <- loadNames("female_first_names.txt")
         people           <- makePeopleStream(lastNames,
                                              femaleFirstNames,
                                              maleFirstNames) }
    yield people
  }

  private def makePeopleStream(lastNames:        Array[String],
                               femaleFirstNames: Array[String],
                               maleFirstNames:   Array[String]):
    Try[Stream[Person]] = {

    def gen(nextID: Int, malesLeft: Int, femalesLeft: Int): Stream[Person] = {
      if (femalesLeft > 0) {
        makePerson(nextID, Gender.Female, femaleFirstNames, lastNames) #::
          gen(nextID + 1, malesLeft, femalesLeft - 1)
      }
      else if (malesLeft > 0) {
        makePerson(nextID, Gender.Male, femaleFirstNames, lastNames) #::
          gen(nextID + 1, malesLeft - 1, femalesLeft)
      }
      else
        Stream.Empty
    }

    val totalMales = (params.totalPeople * params.malePercent) / 100
    val w = (params.totalPeople * params.femalePercent) / 100
    val totalFemales = w + math.abs(params.totalPeople - totalMales - w)
    msg.verbose(s"Generating ${params.totalPeople} people" +
                s"($totalFemales females, $totalMales males)")
    Try {
      gen(1, totalMales, totalFemales)
    }
  }


  private def makeDate(year: Int, month: Int, day: Int): Date = {
    val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    c.set(Calendar.YEAR, year)
    c.set(Calendar.MONTH, month)
    c.set(Calendar.DAY_OF_MONTH, day)
    c.set(Calendar.HOUR, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.getTime
  }

  private def loadNames(resourceName: String): Try[Array[String]] = {
    msg.verbose(s"""Loading resource "$resourceName".""")
    val is = Option(classLoader.getResourceAsStream(resourceName))
    is.map { stream =>
      Try {
        Source.fromInputStream(stream).getLines.toArray
      }
    }
    .getOrElse {
      Failure(new IOException(s"""Can't load resource "$resourceName""""))
    }
  }

  private def makePerson(id:         Int,
                         gender:     Gender.Value,
                         firstNames: Array[String],
                         lastNames:  Array[String]): Person = {
    Person(
      id         = id,
      firstName  = RandomUtil.randomChoice(firstNames),
      middleName = RandomUtil.randomChoice(firstNames),
      lastName   = RandomUtil.randomChoice(lastNames),
      gender     = gender,
      birthDate  = generateBirthDate,
      ssn        = makeFakeSSN(),
      salary     = salaryGen.draw.toInt
    )
  }

  private def generateBirthDate: Date = {
    val epoch = RandomUtil.randomLongBetween(minEpoch, maxEpoch)
    new Date(epoch)
  }

  private def makeFakeSSN(): String = {
    val first = RandomUtil.randomChoice(SSNPrefixes)
    val second = RandomUtil.randomIntBetween(10, 99)
    val third = RandomUtil.randomIntBetween(1000, 9999)
    s"$first-$second-$third"
  }
}

