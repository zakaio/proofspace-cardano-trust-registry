



val commonSettings = Seq(
  scalaVersion := "3.6.2",
  organization := "id.proofspace",
  scalacOptions ++= Seq(
    "-Xmax-inlines","100",
    "-Wvalue-discard",
    "-Wnonunit-statement"
  ),
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
                      "io.github.dotty-cps-async" %%% "dotty-cps-async" % "1.0.0",
                      "com.github.rssh" %%% "appcontext" % "0.2.0",
                      "com.outr" %%% "scribe" % "3.15.3",
                      "org.scalameta" %%% "munit" % "1.0.4" % Test
                    )
                )

val tapirVersion = "1.11.9"

lazy val server = project
                 .in(file("server"))
                 .dependsOn(core.jvm)
                 .enablePlugins(JavaAppPackaging)
                 .enablePlugins(JDebPackaging)
                 .settings(commonSettings)
                 .settings(
                   name := "cardano-trustregistry-service",
                   libraryDependencies ++= Seq(
                     "com.bloxbean.cardano" % "cardano-client-lib" % "0.6.3",
                     "com.bloxbean.cardano" % "cardano-client-backend-blockfrost" % "0.6.3",
                     "com.softwaremill.sttp.tapir" %% "tapir-netty-server" % tapirVersion,
                     "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
                     "com.softwaremill.sttp.tapir" %% "tapir-jsoniter-scala" % tapirVersion,
                     "com.softwaremill.sttp.tapir" %% "tapir-pekko-http-server" % tapirVersion,
                     "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
                     "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.30.14",
                     "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.30.14",
                     "ch.qos.logback" % "logback-classic" % "1.5.12",
                     "io.github.dotty-cps-async" %% "dotty-cps-async" % "1.0.0",
                     "com.github.rssh" %%% "appcontext" % "0.2.0",
                     "org.reactivemongo" %% "reactivemongo" % "1.1.0-RC14",
                     "org.scalameta" %% "metaconfig-typesafe-config" % "0.14.0",
                     "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
                     "org.scalatest" %% "scalatest" % "3.2.19" % Test,
                     "com.softwaremill.sttp.client3" %% "jsoniter" % "3.10.1" % Test,
                     "org.scalameta" %%% "munit" % "1.0.4" % Test,
                     "com.dimafeng" %% "testcontainers-scala-mongodb" % "0.41.8" % Test,
                      "com.dimafeng" %% "testcontainers-scala-munit" % "0.41.8" % Test
                   ),
                   Test / fork := true,
                   Test / parallelExecution := false,
                   Compile / doc / sources := Seq(),
                   Compile / mappings := Seq(),
                   packageDoc / mappings := Seq(),
                   maintainer := "tech@proofspace.id",
                   Debian / name := "proofspace-trustregistry",
                   Debian / packageDescription := "Proofspace TrustRegisty Server",
                   Debian / version := "0.0.1",
                   debianPackageDependencies ++= Seq("java17-runtime-headless"),
                   Debian / linuxPackageMappings ++= Seq(
                     packageMapping( (sourceDirectory.value / "main" / "conf" / "proofspace-trustregistry.conf" )
                       -> "/etc/proofspace-trustregistry/config.json").withConfig(),
                     packageMapping( (sourceDirectory.value / "main" / "deb" / "proofspace-trustregistry.service")
                       -> "/etc/systemd/system/proofspace-trustregistry.service" ).withPerms("0644"),
                     packageMapping((sourceDirectory.value / "main" / "conf" / "logback.xml")
                       -> "/etc/proofspace-trustregistry/logback.xml").withConfig(),
                     packageTemplateMapping("/var/log/proofspace-trustregistry/")().withUser("zaka-runner").withGroup("zaka-runner"),
                   )
                 )


lazy val root = project
                .in(file("."))
                .aggregate(core.js,core.jvm,server)
                .settings(
                   publishArtifact := false
                )
