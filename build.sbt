name := "akka-camel-test"

organization := "com.github.dnvriend"

version := "1.0.0"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val akkaVersion = "2.4.2"
  val camelVersion = "2.16.2"
  val rxJavaReactiveStreamsVersion = "1.0.1"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.typesafe.akka" %% "akka-camel" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "org.apache.camel" % "camel-core" % camelVersion,
    "org.apache.camel" % "camel-rx" % camelVersion,
    "io.reactivex" % "rxjava-reactive-streams" % rxJavaReactiveStreamsVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "org.scalatest" %% "scalatest" % "2.2.4" % Test
  )
}

fork in Test := true

javaOptions in Test ++= Seq("-Xms30m","-Xmx30m")

parallelExecution in Test := false

licenses +=("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

// enable scala code formatting //
import scalariform.formatter.preferences._

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)

// enable updating file headers //
import de.heikoseeberger.sbtheader.license.Apache2_0

headers := Map(
  "scala" -> Apache2_0("2016", "Dennis Vriend"),
  "conf" -> Apache2_0("2016", "Dennis Vriend", "#")
)

enablePlugins(AutomateHeaderPlugin)