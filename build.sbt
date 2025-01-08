

val commonSettings = Seq(
  scalaVersion := "3.6.2",
  organization := "id.proofspace"
)

val scalusSettings = Seq(
  autoCompilerPlugins := true,
  addCompilerPlugin("org.scalus" %% "scalus-plugin" % "0.8.4"),
  libraryDependencies += "org.scalus" %%% "scalus" % "0.8.4"
)

lazy val core = crossProject(JSPlatform, JVMPlatform)
                .in(file("core"))
                .settings(commonSettings)
                .settings(scalusSettings)
                .settings(
                   name := "cardano-trustregistry-core",
                    libraryDependencies ++= Seq(
                      "com.github.rssh" %%% "dotty-cps-async" % "0.9.23",
                      "com.github.rssh" %%% "appcontext" % "0.2.0",
                      "com.outr" %%% "scribe" % "3.15.3",
                      "org.scalameta" %%% "munit" % "1.0.4" % Test
                    )
                )

val tapirVersion = "1.11.9"

lazy val server = project
                 .in(file("server"))
                 .dependsOn(core.jvm)
                 .settings(commonSettings)
                 .settings(
                   name := "cardano-trustregistry-service",
                   libraryDependencies ++= Seq(
                     "com.bloxbean.cardano" % "cardano-client-lib" % "0.6.3",
                     "com.bloxbean.cardano" % "cardano-client-backend-blockfrost" % "0.6.3",
                     "com.softwaremill.sttp.tapir" %% "tapir-netty-server" % tapirVersion,
                     "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
                     "com.softwaremill.sttp.tapir" %% "tapir-jsoniter-scala" % tapirVersion,
                     "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.30.14",
                     "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.30.14",
                     "ch.qos.logback" % "logback-classic" % "1.5.12",
                     "com.github.rssh" %%% "dotty-cps-async" % "0.9.23",
                     "com.github.rssh" %%% "appcontext" % "0.2.0",
                     "org.reactivemongo" %% "reactivemongo" % "1.1.0-RC14",
                     "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
                     "org.scalatest" %% "scalatest" % "3.2.19" % Test,
                     "com.softwaremill.sttp.client3" %% "jsoniter" % "3.10.1" % Test,
                     "org.scalameta" %%% "munit" % "1.0.4" % Test
                   )
                 )


lazy val root = project
                .in(file("."))
                .aggregate(core.js,core.jvm,server)
                .settings(
                   publishArtifact := false
                )
