organization in ThisBuild := "stocktrader"

version in ThisBuild := "1.0"
scalaVersion in ThisBuild := "2.13.1"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided"
val playJsonDerivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "6.0.0"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8" % Test

lazy val `reactive-stock-trader-scala` = (project in file("."))
  .aggregate(
    `portfolio-api`, `portfolio-impl`,
    `broker-api`, `broker-impl`,
    `wiretransfer-api`, `wiretransfer-impl`,
    `bff`
  )

lazy val `common-models` = (project in file("common-models"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )

lazy val `portfolio-api` = (project in file("portfolio-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )
  .dependsOn(`common-models`)

lazy val `portfolio-impl` = (project in file("portfolio-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`portfolio-api`, `broker-api`, `wiretransfer-api`)

lazy val `broker-api` = (project in file("broker-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )
  .dependsOn(`common-models`)

lazy val `broker-impl` = (project in file("broker-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`broker-api`, `portfolio-api`)

lazy val `wiretransfer-api` = (project in file("wiretransfer-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )
  .dependsOn(
    `common-models`,
    `portfolio-api`
  )

lazy val `wiretransfer-impl` = (project in file("wiretransfer-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslPubSub,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`wiretransfer-api`)

lazy val `bff` = (project in file("bff"))
  .enablePlugins(PlayScala, LagomPlay)
  .disablePlugins(PlayLayoutPlugin)
  .dependsOn(
    `portfolio-api`,
    `broker-api`,
    `wiretransfer-api`
  )
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslClient, //lagomScaladslServer
      macwire,
      filters
    )
  )
