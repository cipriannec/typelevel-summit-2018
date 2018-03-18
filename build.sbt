lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "org.longcao",
      scalaVersion := "2.11.12",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "typelevel-summit-2018",
    libraryDependencies := Seq(
      // cats
      "org.typelevel"              %% "cats-core"           % "1.0.1",

      // cats testing
      "org.typelevel"              %% "cats-testkit"        % "1.0.1" % "test",
      "org.scalatest"              %% "scalatest"           % "3.0.5" % "test",
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.6" % "test",

      // Spark
      "org.apache.spark"           %% "spark-core"          % "2.2.1",
      "org.apache.spark"           %% "spark-sql"           % "2.2.1"
    )
  )
