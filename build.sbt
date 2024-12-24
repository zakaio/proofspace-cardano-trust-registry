

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
                   name := "cardano-trustregistry-core"
                )
     
lazy val server = project
                 .in(file("server"))
                 .settings(commonSettings)

lazy val blockfrostApi = project
  .in(file("blockfrost-api"))
  .settings(commonSettings)
  .settings(
    openApiInputSpec := s"${baseDirectory.value.getPath}/blockfrost-openapi.yaml",
    openApiGeneratorName := "scala-sttp",
    openApiOutputDir := baseDirectory.value.name,
    openApiIgnoreFileOverride := s"${baseDirectory.in(ThisBuild).value.getPath}/openapi-ignore-file",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client4" %% "core" % "4.0.0-M20",
      "com.softwaremill.sttp.client4" %% "json4s" % "4.0.0-M20",
      "org.json4s" %% "json4s-jackson" % "4.0.3"
    ),
    (compile in Compile) := ((compile in Compile) dependsOn openApiGenerate).value,
    cleanFiles += baseDirectory.value / "src"
  )

lazy val root = project
                .in(file("."))
                .aggregate(core.js,core.jvm,server)
                .settings(
                   publishArtifact := false
                )
