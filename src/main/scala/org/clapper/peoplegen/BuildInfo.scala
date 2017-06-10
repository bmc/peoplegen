package org.clapper.peoplegen

import grizzled.util.withResource

import java.text.SimpleDateFormat
import java.util.{Date, Properties}

import scala.util.Try

/** The build information, laoded from the build-generated properties file.
  */
case class BuildInfo(name:           String,
                     version:        String,
                     buildTimestamp: Date) {

  /** A consolidated string of build information.
    */
  override val toString = {
    val sDate = BuildInfo.DateFormatter.format(buildTimestamp)
    s"$name, $version (built $sDate)"
  }
}

/** Companion object for `BuildInfo`.
  */
object BuildInfo {
  private val DateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

  /** Find the build-generated properties file, load it, and parse its contents
    * into a `BuildInfo` object.
    *
    * @return A `Success` with the `BuildInfo` object or a `Failure` with the
    *         error.
    */
  def load(): Try[BuildInfo] = {
    Try {
      import scala.collection.JavaConverters.propertiesAsScalaMapConverter
      import grizzled.util.CanReleaseResource.Implicits.CanReleaseCloseable

      val url = this.getClass.getClassLoader.getResource("build.properties")
      val props = new Properties

      withResource(url.openStream()) { inputStream =>
        props.load(inputStream)
        ()
      }

      val propsMap = props.asScala.toMap
      val buildTime = new Date(propsMap("buildTime").toLong)

      BuildInfo(name           = propsMap.getOrElse("name", "?"),
                version        = propsMap.getOrElse("version", "?"),
                buildTimestamp = buildTime)
    }
  }
}
