package org.clapper.peoplegen

import java.io.Writer
import java.nio.file.Path

import scala.util.Try

/**
  */
class PeopleWriterSpec extends BaseSpec {

  val TestParams = Params(
    fileFormat     = FileFormat.CSV,
    malePercent    = 50,
    femalePercent  = 50,
    generateSSNs   = true,
    generateHeader = true
  )

  class TestPeopleWriter(val params: Params) extends PeopleWriter with OutputHandler {
    val msg = EmptyMessageHandler
    val outputBuf = new MessageBuffer

    def withOutputFile[T](outputFile: Option[Path])
                         (code: Writer => Try[T]): Try[T] = {
      code(outputBuf.asWriter)
    }
  }

  "thing" should "thing" in {
    val params = TestParams.copy(totalPeople = 1)
    val generator = new PeopleGenerator(params, EmptyMessageHandler)
    val t = generator.generatePeople
    t shouldBe 'success

    val initialStream = t.get
    val people = initialStream.toVector
    val reStream = Stream(people: _*)
    val writer = new TestPeopleWriter(params)
    writer.write(reStream) shouldBe 'success

  }
}
