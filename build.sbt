name := "SoDA"

version := "1.0"

scalaVersion := "2.10.6"

organization := "com.elsevier"

//sbtVersion := "0.13.1"

enablePlugins(JettyPlugin)

containerConfigFile := Some(file("src/main/resources/jetty.xml"))

libraryDependencies ++= Seq(
    // main
    "org.openrdf.sesame" % "sesame-rio" % "2.7.10",
    "org.openrdf.sesame" % "sesame-rio-ntriples" % "2.7.10",
    "com.amazonaws" % "aws-java-sdk" % "1.9.39",
    "org.apache.solr" % "solr-solrj" % "5.0.0",
    "org.apache.lucene" % "lucene-core" % "5.0.0",
    "org.apache.lucene" % "lucene-analyzers-common" % "5.0.0",
    "org.apache.opennlp" % "opennlp-maxent" % "3.0.3",
    "org.apache.opennlp" % "opennlp-tools" % "1.5.3",
    "com.aliasi" % "lingpipe" % "4.0.1" from "http://clojars.org/repo/com/aliasi/lingpipe/4.0.1/lingpipe-4.0.1.jar",
    "log4j" % "log4j" % "1.2.14",
    // web
    "org.json4s" %% "json4s-native" % "3.2.10",
    "org.json4s" %% "json4s-jackson" % "3.2.10" % "provided",
    "org.springframework" % "spring-webmvc" % "4.0.0.RELEASE",
    "jfree" % "jfreechart" % "1.0.13",
    "org.apache.commons" % "commons-lang3" % "3.0",
    "net.sourceforge.collections" % "collections-generic" % "4.01",
    "commons-beanutils" % "commons-beanutils" % "1.8.3",
    "commons-io" % "commons-io" % "2.4",
    // client
    "org.apache.httpcomponents" % "httpclient" % "4.0-alpha4",
    "dom4j" % "dom4j" % "1.6.1",
    // local container
    "org.eclipse.jetty" % "jetty-webapp" % "9.3.0.M1" % "compile,container",
    "org.eclipse.jetty" % "jetty-jsp" % "9.3.0.M1" % "container",
    // test
    "com.novocode" % "junit-interface" % "0.8" % "test"
)

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

