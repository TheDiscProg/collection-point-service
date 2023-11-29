import sbt._

object Dependencies {

  lazy val all = Seq(
    "Shareprice" %% "shareprice-config" % "0.9.4",
    "simex" %% "simex-messaging" % "0.7.3",
    "org.http4s" %% "http4s-dsl" % "0.23.18",
    "org.http4s" %% "http4s-ember-server" % "0.23.18",
    "org.http4s" %% "http4s-ember-client" % "0.23.18",
    "org.http4s" %% "http4s-circe" % "0.23.18",
    "io.scalaland" %% "chimney" % "0.7.4",
    "ch.qos.logback" % "logback-classic" % "1.4.11",
    "org.typelevel" %% "log4cats-core" % "2.6.0",
    "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
    "org.typelevel" %% "munit-cats-effect-2" % "1.0.7" % Test,
    "org.scalatest" %% "scalatest" % "3.2.15" % Test,
    "org.scalatestplus" %% "mockito-4-6" % "3.2.15.0" % Test
  )

  lazy val it = Seq(
    "ch.qos.logback" % "logback-classic" % "1.4.11",
    "org.testcontainers" % "mysql" % "1.19.1"
  )
}
