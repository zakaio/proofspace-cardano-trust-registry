

val commonSettings = Seq(
  scalaVersion := "3.3.4",
  organization := "id.proofspace"
)

val scalusSettings = Seq(
  autoCompilerPlugins := true,
  addCompilerPlugin("org.scalus" %% "scalus-plugin" % "0.8.2"),
  libraryDependencies += "org.scalus" %%% "scalus" % "0.8.2"
)

lazy val core = crossProject(JSPlatform, JVMPlatform)
                .in(file("core"))
                .settings(commonSettings)
                .settings(scalusSettings)
                .settings(
                   name := "cardano-trustregistry-core",
                )
     
lazy val server = project
                 .in(file("server"))
                 .settings(commonSettings)
                 .settings(
                   name := "cardano-trustregistry-service",
                   libraryDependencies ++= Seq(
                     "com.bloxbean.cardano" % "cardano-client-lib" % "0.5.1",
                     "com.bloxbean.cardano" % "cardano-client-backend-blockfrost" % "0.5.1"
                   )
                 )


lazy val root = project
                .in(file("."))
                .aggregate(core.js,core.jvm,server)
                .settings(
                   publishArtifact := false
                )
