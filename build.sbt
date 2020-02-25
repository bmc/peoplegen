import grizzled.file.{util => fileutil}
import scala.sys.process._
import scala.util.control.NonFatal

lazy val Name = "peoplegen"
lazy val Version = "2.1.4"

enablePlugins(BuildInfoPlugin)

organization := "org.clapper"
scalaVersion := "2.13.1"
version      := Version
name         := Name

scalacOptions := Seq("-deprecation", "-feature", "-unchecked")

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt"          % "4.0.0-RC2",
  "org.clapper"      %% "grizzled-scala" % "4.10.0",
  "io.spray"         %% "spray-json"     % "1.3.5",
  "org.scalanlp"     %% "breeze"         % "1.0",
  "org.scalatest"    %% "scalatest"      % "3.1.1" % Test
)

// InstallDir defines where to install the software. The BAT or shell script
// will be installed in $InstallDir/bin. The jar will be installed in
// $InstallDir/libexec. You if you set the environment variable INSTALL_DIR,
// it overrides this setting. It defaults to $HOME/local (%HOME%\local on
// Windows).
val installDir = settingKey[Option[String]]("Installation directory")
installDir := Option(System.getProperty("user.home")).map(fileutil.joinPath(_, "local"))

// Example of setting your own:
// installDir := Some("/usr/local")

// sbt-buildinfo configuration
buildInfoKeys := Seq[BuildInfoKey](
  name, version,
  BuildInfoKey.action("buildTime") { System.currentTimeMillis },
  BuildInfoKey.action("buildHash") {
    try {
      "git rev-parse --short=10 HEAD".!!.stripLineEnd
    }
    catch {
      case NonFatal(e) =>
        System.err.println(s"Unable to get git hash: ${e}")
        "<unknown-git-hash>"
    }
  }
)
buildInfoPackage := "org.clapper.peoplegen"

addCommandAlias("fatjar", "assembly")

// ---------------------------------------------------------------------------
// Tasks
// ---------------------------------------------------------------------------

// Install Task

addCommandAlias("install", ";assembly;installFiles")

def abort(msg: String): Unit = throw new Exception(msg)

val installRoot = settingKey[String]("(INTERNAL) get the install root")
installRoot := {
  // Get the installation directory.
  val oInstallRoot = Option(System.getenv("INSTALL_DIR")).orElse(installDir.value)
  if (oInstallRoot.isEmpty) {
    abort("INSTALL_DIR is undefined, and can't find HOME directory.")
  }

  oInstallRoot.get
}


val installationDir = taskKey[Unit]("Display the installation directory.")
installationDir := { println(installRoot.value) }

val installFiles = taskKey[Unit]("Install the software.")

installFiles := {
  import grizzled.sys.{os, OperatingSystem}
  import scala.io.Source
  import scala.language.postfixOps
  import java.io.File
  import grizzled.util.CanReleaseResource.Implicits.CanReleaseCloseable

  // Find the assembly jar file
  val baseScalaVersion = scalaVersion.value.split("""\.""").take(2).mkString(".")
  val jar = new File(
    (baseDirectory.value / "target" / s"scala-$baseScalaVersion" /
      (assemblyJarName in assembly).value).toString
  )

  if ((! jar.exists) || (! jar.isFile))
    abort(s"$jar does not exist or isn't a file")

  val scriptDir = baseDirectory.value / "src" / "main" / "scripts"

  def mkdirp(dir: String): Unit = new File(dir).mkdirs()

  def installFiles(scriptTemplate: File, scriptDestName: String): (File, File) = {
    import grizzled.util._
    import grizzled.file.Implicits.GrizzledFile
    import java.io.{FileWriter,PrintWriter}

    val installRootPath = installRoot.value

    println(s"""Installing under "$installRootPath".""")
    val jarDest = fileutil.joinPath(
      installRootPath,
      "libexec",
      fileutil.basename(jar.getPath)
    )
    if (File.separator == "\\") jarDest.replace("\\", "\\\\")
    println(s"""Copying $jar to "$jarDest".""")
    mkdirp(fileutil.dirname(jarDest))
    fileutil.copyFile(jar.getPath, jarDest).get

    val scriptDest = fileutil.joinPath(installRootPath, "bin", scriptDestName)

    println(s"""Installing ${scriptTemplate.basename} to "$scriptDest".""")
    mkdirp(fileutil.dirname(scriptDest))
    withResource(new PrintWriter(new FileWriter(scriptDest))) { out =>
      val javaHome = System.getProperty("java.home")
      for (line <- Source.fromFile(scriptTemplate).getLines) {
        val lineOut = line.replace("@JAR@", jarDest)
          .replace("@JAVA_HOME@", javaHome)
        out.println(lineOut)
      }
    }

    (new File(scriptDest), new File(jarDest))
  }

  def doUnixLikeInstall() = {
    val (script, _) = installFiles(
      scriptTemplate = new File((scriptDir / "peoplegen.sh").toString),
      scriptDestName = "peoplegen"
    )

    s"chmod +x $script".!!
  }

  def doWindowsInstall() = {
    installFiles(
      scriptTemplate = new File((scriptDir / "peoplegen.bat").toString),
      scriptDestName = "peoplegen.bat"
    )
  }

  os match {
    case OperatingSystem.Mac     => doUnixLikeInstall()
    case OperatingSystem.Windows => doWindowsInstall()
    case OperatingSystem.Posix   => doUnixLikeInstall()
    case os @ _                  => abort(s"Can't install on $os")
  }
}
