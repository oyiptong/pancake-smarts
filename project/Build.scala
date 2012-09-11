import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "pancake-smarts"
    val appVersion      = "0.1-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "org.elasticsearch" % "elasticsearch" % "0.19.9"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      // Add your own project settings here      
      resolvers += "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases"
    )

}
