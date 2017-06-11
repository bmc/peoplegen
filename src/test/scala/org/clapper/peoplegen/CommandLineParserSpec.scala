package org.clapper.peoplegen
import java.io.{ByteArrayOutputStream, PrintStream}
import java.util.Date

import scala.util.{Failure, Success}

class CommandLineParserSpec extends BaseSpec {

  object TestCommandLineParser extends CommandLineParserBase {
    val stdoutBuffer = new ByteArrayOutputStream
    val stderrBuffer = new ByteArrayOutputStream

    override def outputStream: PrintStream = new PrintStream(stdoutBuffer)
    override def errorStream: PrintStream = new PrintStream(stderrBuffer)

    def outputString: String = stdoutBuffer.toString(defaultEncoding)
    def errorString: String = stderrBuffer.toString(defaultEncoding)

    private def defaultEncoding = {
      Option(System.getProperty("file.encoding")).getOrElse("UTF-8")
    }
  }

  val TestBuildInfo = new BuildInfo(name           = "peoplegen",
                                    version        = "test",
                                    buildTimestamp = new Date)

  "parseParams" should "fail for an incorrect option" in {
    val args = Array("-x")

    TestCommandLineParser.parseParams(args, TestBuildInfo) shouldBe 'failure
    TestCommandLineParser.errorString should include ("Usage")
  }

  it should "succeed with valid parameters" in {
    val args = Array("-F", "json", "--female", "10", "100")
    TestCommandLineParser.parseParams(args, TestBuildInfo) shouldBe 'success
  }

  it should "fail if male and female percentages are less than 100" in {
    val args = Array("--female", "10", "--male", "10", "1000")
    TestCommandLineParser.parseParams(args, TestBuildInfo) shouldBe 'failure
  }

  it should "fail if male and female percentages are more than 100" in {
    val args = Array("--female", "10", "--male", "100", "1000")
    TestCommandLineParser.parseParams(args, TestBuildInfo) shouldBe 'failure
  }

  it should "succeed if male and female percentages are exactly 100" in {
    val args = Array("--male", "10", "--female", "90", "1000")
    TestCommandLineParser.parseParams(args, TestBuildInfo) shouldBe 'success
  }

  it should "succeed if only --male is specified" in {
    val args = Array("--male", "10", "100")
    val t = TestCommandLineParser.parseParams(args, TestBuildInfo)
    t shouldBe 'success
    val params = t.get
    params.malePercent shouldBe 10
    params.femalePercent shouldBe 90
  }

  it should "succeed if only --female is specified" in {
    val args = Array("--female", "80", "100")
    val t = TestCommandLineParser.parseParams(args, TestBuildInfo)
    t shouldBe 'success
    val params = t.get
    params.malePercent shouldBe 20
    params.femalePercent shouldBe 80
  }

  it should "fail if --year-min exceeds --year-max" in {
    val args = Array("--year-min", "1965", "--year-max", "1925")
    val t = TestCommandLineParser.parseParams(args, TestBuildInfo)
    t shouldBe 'failure
  }

  it should "succeed if --year-min doesn't exceed --year-max" in {
    val args = Array("--year-min", "1920", "--year-max", "1990", "1000")
    val t = TestCommandLineParser.parseParams(args, TestBuildInfo)
    t shouldBe 'success
  }
}
