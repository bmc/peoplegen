package org.clapper.peoplegen

import java.io.Writer
import java.nio.file.Path
import java.text.SimpleDateFormat

import org.clapper.peoplegen.converters.{CSVConverter, JSONConverter}

import scala.util.Try

/**
  */
class PeopleWriterSpec extends BaseSpec {

  val Delimiter = ','

  val TestParams = Params(
    fileFormat       = FileFormat.CSV,
    malePercent      = 50,
    femalePercent    = 50,
    generateSSNs     = true,
    generateHeader   = true,
    generateSalaries = true,
    columnSep        = Delimiter
  )

  val TestBirthDateFormat = new SimpleDateFormat("yyyy-MM-dd")

  class TestPeopleWriter(val params: Params) extends PeopleWriter with OutputHandler {
    val msg = EmptyMessageHandler
    val outputBuf = new MessageBuffer
    val BirthDateFormat = TestBirthDateFormat

    def withOutputFile[T](outputFile: Option[Path])
                         (code: Writer => Try[T]): Try[T] = {
      code(outputBuf.asWriter)
    }
  }

  class TestCSVConverter extends CSVConverter(
    headerFormat  = TestParams.headerFormat,
    delimiter     = TestParams.columnSep,
    writeHeader   = TestParams.generateHeader,
    writeSSNs     = TestParams.generateSSNs,
    writeSalaries = TestParams.generateSalaries,
    dateFormat    = TestBirthDateFormat,
    msg           = EmptyMessageHandler
  )

  "write" should "produce the same CSV output as the CSV converter" in {
    val params = TestParams.copy(totalPeople = 1)
    val generator = new PeopleGenerator(params, EmptyMessageHandler)
    val t = generator.generatePeople
    t shouldBe 'success

    val initialStream = t.get
    val people = initialStream.toVector
    val writer = new TestPeopleWriter(params)
    writer.write(Stream(people: _*)) shouldBe 'success

    val converter = new TestCSVConverter
    val writtenCSV = writer.outputBuf.asString
    val tExpectedCSV =
      for { hdr: String <- Try { converter.header }
            // there's only one person record in this test
            row: String <- converter.convertPerson(people.head) }
        yield s"$hdr\n$row\n"

    tExpectedCSV shouldBe 'success
    val expectedCSV = tExpectedCSV.get.mkString("")

    writtenCSV shouldBe expectedCSV
  }

  it should "produce the same compact JSON row output as the JSON converter" in {
    val params = TestParams.copy(
      totalPeople = 2,
      fileFormat  = FileFormat.JSON,
      jsonFormat  = JSONFormat.AsRows
    )
    val generator = new PeopleGenerator(params, EmptyMessageHandler)
    val t = generator.generatePeople
    t shouldBe 'success

    val initialStream = t.get
    val people = initialStream.toVector
    val writer = new TestPeopleWriter(params)
    writer.write(Stream(people: _*)) shouldBe 'success

    val converter = new JSONConverter(
      headerFormat  = TestParams.headerFormat,
      jsonFormat    = JSONFormat.AsRows,
      pretty        = false,
      writeSSNs     = TestParams.generateSSNs,
      writeSalaries = TestParams.generateSalaries,
      dateFormat    = TestBirthDateFormat,
      msg           = EmptyMessageHandler
    )

    val writtenJSON = writer.outputBuf.asString
    val tExpectedJSON = converter
      .convertPeople(Stream(people: _*))
      .map { stringStream => stringStream.mkString("\n") + "\n" }

    tExpectedJSON shouldBe 'success
    val expectedJSON = tExpectedJSON.get

    writtenJSON shouldBe expectedJSON
  }
}
