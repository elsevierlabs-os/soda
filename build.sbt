name := "SoDA"

version := "2.0"

scalaVersion := "2.12.6"

organization := "com.elsevier"

enablePlugins(JettyPlugin)

libraryDependencies ++= Seq(
    // web-mvc
    "org.springframework" % "spring-webmvc" % "5.0.7.RELEASE",
    "org.springframework" % "spring-context" % "5.0.7.RELEASE",
    "javax.servlet" % "javax.servlet-api" % "4.0.1" % "provided",
    // json
    "com.google.code.gson" % "gson" % "2.8.5",
    "commons-io" % "commons-io" % "2.4",
    // distance metric
    "org.apache.commons" % "commons-text" % "1.4",
    // http
    "com.softwaremill.sttp" %% "core" % "1.2.0-RC6",
    "com.softwaremill.sttp" %% "json4s" % "1.2.0-RC6",
    // solr
    "org.apache.solr" % "solr-solrj" % "7.3.1",
    // logging
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "org.slf4j" % "slf4j-log4j12" % "1.7.25",
    // test
    "com.novocode" % "junit-interface" % "0.11" % Test
)

