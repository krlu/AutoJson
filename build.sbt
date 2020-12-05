scalaVersion := "2.13.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "auto-json",
    version := "0.1.0",

    libraryDependencies += "fr.inria.gforge.spoon" % "spoon-core" % "8.2.0",
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.1"
)
