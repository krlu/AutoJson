scalaVersion := "2.13.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "auto-json",
    version := "0.1.0",

    libraryDependencies ++= List(
      "fr.inria.gforge.spoon" % "spoon-core" % "8.2.0",
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "org.scalatest" %% "scalatest" % "3.2.3" % Test,
      "org.json4s" %% "json4s-core" % "3.7.0-M7",
      "org.json4s" %% "json4s-native" % "3.7.0-M7",
      "org.json4s" %% "json4s-jackson" % "3.7.0-M7",
      "org.slf4j" % "slf4j-simple" % "1.7.30" % Test,
      "org.reflections" % "reflections" % "0.9.12",
    )
  )
