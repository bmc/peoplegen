import grizzled.file.{util => fileutil}

name := """peoplegen"""
version := "2.1.0"

scalaVersion := "2.11.11"
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

// InstallDir defines where to install the software. The BAT or shell script
// will be installed in $InstallDir/bin. The jar will be installed in
// $InstallDir/libexec. You if you set the environment variable INSTALL_DIR,
// it overrides this setting. It defaults to $HOME/local (%HOME%\local on
// Windows).
val InstallDir = settingKey[Option[String]]("Installation directory")
InstallDir := Option(System.getProperty("user.home")).map(fileutil.joinPath(_, "local"))

// Example of setting your own:
// InstallDir := Some("/usr/local")

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt"          % "3.6.0",
  "org.clapper"      %% "grizzled-scala" % "4.2.0",
  "io.spray"         %% "spray-json"     % "1.3.3",
  "org.scalanlp"     %% "breeze"         % "0.13.1",
  "org.scalatest"    %% "scalatest"      % "3.0.1" % Test
)

// ---------------------------------------------------------------------------
// Tasks
// ---------------------------------------------------------------------------

// Simple task to create a Java properties file from the build information in
// this file. This file is loaded by com.databricks.training.peoplegen.BuildInfo.

val makeBuildInfo = taskKey[File]("Create the build info file")

makeBuildInfo := {
  import java.io.{File => JFile, FileOutputStream}
  import java.util.{Date, Properties}

  val f = new JFile(
    (baseDirectory.value / "src" / "main" / "resources" / "build.properties")
      .toString
  )

  val props = new Properties
  props.put("buildTime", (new Date).getTime.toString)
  props.put("version", version.value)
  props.put("name", name.value)

  val out = new FileOutputStream(f)
  props.store(out, s"${name.value} build information metadata file.")
  f
}

// Automatically build the properties file during compilation.
compile in Compile := ((compile in Compile) dependsOn makeBuildInfo).value

// Install Task


addCommandAlias("install", ";assembly;installFiles")

def abort(msg: String): Unit = throw new Exception(msg)

val _installRoot = settingKey[String]("(INTERNAL) get the install root")
_installRoot := {
  // Get the installation directory.
  val oInstallRoot = Option(System.getenv("INSTALL_DIR")).orElse(InstallDir.value)
  if (oInstallRoot.isEmpty) {
    abort("INSTALL_DIR is undefined, and can't find HOME directory.")
  }

  oInstallRoot.get
}


val installationDir = taskKey[Unit]("Display the installation directory.")
installationDir := { println(_installRoot.value) }

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

    val installRoot = _installRoot.value

    println(s"""Installing under "$installRoot".""")
    val jarDest = fileutil.joinPath(installRoot,
      "libexec",
      fileutil.basename(jar.getPath))
    if (File.separator == "\\") jarDest.replace("\\", "\\\\")
    println(s"""Copying $jar to "$jarDest".""")
    mkdirp(fileutil.dirname(jarDest))
    fileutil.copyFile(jar.getPath, jarDest).get

    val scriptDest = fileutil.joinPath(installRoot, "bin", scriptDestName)

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

    s"chmod +x $script" !!
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
