scalariformSettings

lazy val akkaV = "2.3.14"
lazy val akkaStreamsV = "1.0"

lazy val root = (project in file("."))
  .settings(
    scalaVersion  := "2.11.7",
    name := "consierge",
    organization  := "ensime",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaV,
      "com.typesafe.akka" %% "akka-slf4j" % akkaV,
      "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamsV,
      "com.typesafe.akka" %% "akka-http-core-experimental" % akkaStreamsV,
      "com.typesafe.akka" %% "akka-http-experimental" % akkaStreamsV,
      "com.typesafe.play" %% "play-json" % "2.4.3",
      "com.typesafe.scala-logging" %% "scala-logging"   % "3.1.0",
      "ch.qos.logback" % "logback-classic" % "1.1.2",
      "joda-time" % "joda-time" % "2.9.1"
    ) ++ Seq(
      "org.scalatest" %% "scalatest" % "2.2.4",
      "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2",
      "com.typesafe.akka" %% "akka-stream-testkit-experimental" % akkaStreamsV,
      "com.typesafe.akka" %% "akka-http-testkit-experimental" % akkaStreamsV
    ).map { _ % "test" }
  )
