name := "finagle-app"
version := "0.1.0"
scalaVersion := "2.10.5"

resolvers ++= Seq(
  "mvnrepository" at "https://mvnrepository.com/artifact",
  "central_maven_2" at "http://central.maven.org/maven2/",
  "twitter" at "http://maven.twttr.com/")

resolvers += Resolver.sonatypeRepo("releases")

/** versions */
val finagleVersion  = "6.35.0"
val jodaTimeVersion = "2.9.9"
val json4sVersion   = "3.5.3"
val twitterServerVersion = "1.20.0"

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-native" % json4sVersion,
  "org.json4s" %% "json4s-jackson" % json4sVersion,
  "com.twitter" % "finagle-http_2.10" % finagleVersion,
  "joda-time" % "joda-time" % jodaTimeVersion,
  "com.twitter" % "twitter-server_2.10" % twitterServerVersion
)
