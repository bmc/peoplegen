package org.clapper.peoplegen

import java.io.IOException

import grizzled.random.RandomUtil
import java.util.{Calendar, Date, TimeZone}

import scala.io.Source
import scala.util.{Failure, Try}

/**
  */
class PeopleGenerator(params: Params) {

  private val classLoader = this.getClass.getClassLoader
  private val SSNPrefixes = (900 to 999).toArray :+ 666
  private val today       = new Date
  private val todayCal    = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
  private val minBirthDate = makeDate(params.minimumYear, 0, 1)
  private val maxBirthDate = makeDate(params.maximumYear, 12, 31)
  private val minEpoch     = minBirthDate.getTime
  private val maxEpoch     = maxBirthDate.getTime

  def generatePeople: Try[Seq[Person]] = {
    for {lastNames        <- loadNames("last_names.txt")
         maleFirstNames   <- loadNames("male_first_names.txt")
         femaleFirstNames <- loadNames("female_first_names.txt")
         people           <- generatePeople(maleFirstNames,
                                            femaleFirstNames,
                                            lastNames) }
    yield people
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

  private def generatePeople(maleFirstNames:   Array[String],
                             femaleFirstNames: Array[String],
                             lastNames:        Array[String]): Try[Seq[Person]] = {
    Try {

      val totalMen = (params.totalPeople * params.malePercent) / 100
      val w = (params.totalPeople * params.femalePercent) / 100
      val totalWomen = w + math.abs(params.totalPeople - totalMen - w)

      val womenNoSalaries = (1 to totalWomen).map { _ =>
        generatePersonNoSalary("F", femaleFirstNames, lastNames)
      }
      val menNoSalaries = (1 to totalMen).map { _ =>
        generatePersonNoSalary("M", maleFirstNames, lastNames)
      }
      val peopleNoSalaries = womenNoSalaries ++ menNoSalaries

      // Add the salaries
      val peopleWithSalaries = params.salaryInfo.map { si =>
        val salaries = generateSalaries(params.totalPeople, si)
        peopleNoSalaries.zip(salaries).map { case (person, salary) =>
            person.copy(salary = Some(salary))
        }
      }
      .getOrElse(peopleNoSalaries)

      peopleWithSalaries
    }
  }

  private def generatePersonNoSalary(gender:     String,
                                     firstNames: Array[String],
                                     lastNames:  Array[String]): Person = {
    val ssn = if (params.generateSSNs)
      Some(makeFakeSSN())
    else
      None

    Person(
      firstName = RandomUtil.randomChoice(firstNames),
      middleName = RandomUtil.randomChoice(firstNames),
      lastName   = RandomUtil.randomChoice(lastNames),
      gender     = gender,
      birthDate  = generateBirthDate,
      ssn        = ssn,
      salary     = None
    )
  }

  private def generateBirthDate: Date = {
    val epoch = RandomUtil.randomLongBetween(minEpoch, maxEpoch)
    new Date(epoch)
  }

  private def generateSalaries(total: Int, info: SalaryInfo): Seq[Int] = {
    // Attempt a gaussian (normal) distribution.
    import breeze.stats.distributions.Gaussian
    val g = Gaussian(mu = info.mean, sigma = info.sigma)

    g.sample(total).map(_.toInt)
  }

  private def makeFakeSSN(): String = {
    val first = RandomUtil.randomChoice(SSNPrefixes)
    val second = RandomUtil.randomIntBetween(10, 99)
    val third = RandomUtil.randomIntBetween(1000, 9999)
    s"$first-$second-$third"
  }
}

