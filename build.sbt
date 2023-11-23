ThisBuild / organization := "simex"

ThisBuild / version := "0.1.0"

lazy val commonSettings = Seq(
  scalaVersion := "2.13.10",
  libraryDependencies ++= Dependencies.all,
  resolvers += Resolver.githubPackages("TheDiscProg"),
  githubOwner := "TheDiscProg",
  githubRepository := "collection-point-service",
  addCompilerPlugin(
    ("org.typelevel" %% "kind-projector" % "0.13.2").cross(CrossVersion.full)
  ),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
)

lazy val base = (project in file("base"))
  .settings(
    commonSettings,
    name := "collection-point-service-base",
    scalacOptions ++= Scalac.options,
    coverageExcludedPackages := Seq(
      "<empty>",
      ".*.entities.*"
    ).mkString(";"),
    publish / skip := true
  )

lazy val guardrail = (project in file("guardrail"))
  .settings(
    commonSettings,
    name := "collection-point-service-guardrail",
    Compile / guardrailTasks := List(
      ScalaServer(
        file("swagger.yaml"),
        pkg = "simex.guardrail",
        framework = "http4s",
        tracing = false,
        imports = List(
          "eu.timepit.refined.types.string.NonEmptyString"
        )
      )
    ),
    publish / skip := true,
    coverageExcludedPackages := Seq(
      "<empty>",
      ".*guardrail.*"
    ).mkString(";")
  )
  .dependsOn(base % "test->test; compile->compile")

lazy val root = (project in file("."))
  .enablePlugins(
    ScalafmtPlugin,
    JavaAppPackaging,
    UniversalPlugin,
    DockerPlugin
  )
  .settings(
    commonSettings,
    name := "collection-point-service",
    Compile / doc / sources := Seq.empty,
    scalacOptions ++= Scalac.options,
    coverageExcludedPackages := Seq(
      "<empty>"
    ).mkString(";"),
    coverageExcludedFiles := Seq(
      "<empty>",
      ".*MainApp.*",
      ".*AppServer.*"
    ).mkString(";"),
    coverageFailOnMinimum := true,
    coverageMinimumStmtTotal := 92,
    coverageMinimumBranchTotal := 100,
    Compile / mainClass := Some("simex.MainApp"),
    Docker / packageName := "collection-point-service",
    Docker / dockerUsername := Some("ramindur"),
    Docker / defaultLinuxInstallLocation := "/opt/collection-point-service",
    dockerBaseImage := "eclipse-temurin:17-jdk-jammy",
    dockerExposedPorts ++= Seq(8300),
    dockerExposedVolumes := Seq("/opt/docker/.logs", "/opt/docker/.keys")
  )
  .aggregate(base, guardrail)
  .dependsOn(base % "test->test; compile->compile")
  .dependsOn(guardrail % "test->test; compile->compile")

// Put here as database repository tests may hang but remove for none db applications
parallelExecution := false

addCommandAlias("cleanTest", ";clean;scalafmt;test:scalafmt;test;")
addCommandAlias("cleanCoverage", ";clean;scalafmt;test:scalafmt;coverage;test;coverageReport;")