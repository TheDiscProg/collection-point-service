import sbt._

object Dependencies {

  lazy val all = Seq(
    "Shareprice" %% "shareprice-config" % "0.8.0",
    "org.http4s" %% "http4s-dsl" % "0.23.18",
    "org.http4s" %% "http4s-ember-server" % "0.23.18",
    "org.http4s" %% "http4s-ember-client" % "0.23.18",
    "org.http4s" %% "http4s-circe" % "0.23.18",
    "org.typelevel" %% "munit-cats-effect-2" % "1.0.7" % Test,
    "org.scalatest" %% "scalatest" % "3.2.15" % Test,
    "org.scalatestplus" %% "mockito-4-6" % "3.2.15.0" % Test
  )
}
