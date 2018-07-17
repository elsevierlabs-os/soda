# classpath setup
# you may need to change this to suit your environment. In my case, 
# these classpaths point to the same ivy2 cache that sbt reads from.
# A more production like setup might have all the JARs copied to a 
# flat directory instead.

SCALA_LIB_PATH=$HOME/.sbt/boot/scala-2.12.6/lib/scala-library.jar
SODA_JAR_PATH=../../../target/scala-2.12/soda_2.12-2.0.jar 

REPOSITORY=$HOME/.ivy2/cache

CLASSPATH=$SCALA_LIB_PATH:\
$SODA_JAR_PATH:\
$REPOSITORY/org.springframework/spring-webmvc/jars/spring-webmvc-5.0.7.RELEASE.jar:\
$REPOSITORY/org.springframework/spring-aop/jars/spring-aop-5.0.7.RELEASE.jar:\
$REPOSITORY/org.springframework/spring-beans/jars/spring-beans-5.0.7.RELEASE.jar:\
$REPOSITORY/org.springframework/spring-core/jars/spring-core-5.0.7.RELEASE.jar:\
$REPOSITORY/org.springframework/spring-jcl/jars/spring-jcl-5.0.7.RELEASE.jar:\
$REPOSITORY/org.springframework/spring-context/jars/spring-context-5.0.7.RELEASE.jar:\
$REPOSITORY/org.springframework/spring-expression/jars/spring-expression-5.0.7.RELEASE.jar:\
$REPOSITORY/org.springframework/spring-web/jars/spring-web-5.0.7.RELEASE.jar:\
$REPOSITORY/javax.servlet/jstl/jars/jstl-1.2.jar:\
$REPOSITORY/com.google.code.gson/gson/jars/gson-2.8.5.jar:\
$REPOSITORY/com.typesafe.akka/akka-actor_2.12/jars/akka-actor_2.12-2.5.13.jar:\
$REPOSITORY/com.typesafe/config/bundles/config-1.3.2.jar:\
$REPOSITORY/org.scala-lang.modules/scala-java8-compat_2.12/bundles/scala-java8-compat_2.12-0.8.0.jar:\
$REPOSITORY/com.softwaremill.sttp/core_2.12/jars/core_2.12-1.2.0-RC6.jar:\
$REPOSITORY/com.softwaremill.sttp/json4s_2.12/jars/json4s_2.12-1.2.0-RC6.jar:\
$REPOSITORY/org.json4s/json4s-native_2.12/jars/json4s-native_2.12-3.5.4.jar:\
$REPOSITORY/org.json4s/json4s-core_2.12/jars/json4s-core_2.12-3.5.4.jar:\
$REPOSITORY/org.json4s/json4s-ast_2.12/jars/json4s-ast_2.12-3.5.4.jar:\
$REPOSITORY/org.json4s/json4s-scalap_2.12/jars/json4s-scalap_2.12-3.5.4.jar:\
$REPOSITORY/com.thoughtworks.paranamer/paranamer/bundles/paranamer-2.8.jar:\
$REPOSITORY/org.scala-lang.modules/scala-xml_2.12/bundles/scala-xml_2.12-1.0.6.jar:\
$REPOSITORY/org.apache.commons/commons-text/jars/commons-text-1.4.jar:\
$REPOSITORY/org.apache.commons/commons-lang3/jars/commons-lang3-3.7.jar:\
$REPOSITORY/org.apache.solr/solr-solrj/jars/solr-solrj-7.3.1.jar:\
$REPOSITORY/commons-io/commons-io/jars/commons-io-2.5.jar:\
$REPOSITORY/org.apache.commons/commons-math3/jars/commons-math3-3.6.1.jar:\
$REPOSITORY/org.apache.httpcomponents/httpclient/jars/httpclient-4.5.3.jar:\
$REPOSITORY/org.apache.httpcomponents/httpcore/jars/httpcore-4.4.6.jar:\
$REPOSITORY/org.apache.httpcomponents/httpmime/jars/httpmime-4.5.3.jar:\
$REPOSITORY/org.apache.zookeeper/zookeeper/jars/zookeeper-3.4.11.jar:\
$REPOSITORY/org.codehaus.woodstox/stax2-api/bundles/stax2-api-3.1.4.jar:\
$REPOSITORY/org.codehaus.woodstox/woodstox-core-asl/jars/woodstox-core-asl-4.4.1.jar:\
$REPOSITORY/org.noggit/noggit/jars/noggit-0.8.jar:\
$REPOSITORY/org.slf4j/jcl-over-slf4j/jars/jcl-over-slf4j-1.7.24.jar:\
$REPOSITORY/org.slf4j/slf4j-api/jars/slf4j-api-1.7.25.jar:\
$REPOSITORY/org.slf4j/slf4j-log4j12/jars/slf4j-log4j12-1.7.25.jar:\
$REPOSITORY/log4j/log4j/bundles/log4j-1.2.17.jar
#echo $CLASSPATH

LEXICON_NAME=$1
INPUT_PATH=$2
NUM_WORKERS=$3
scala -cp $CLASSPATH com.elsevier.soda.SodaBulkLoader $LEXICON_NAME $INPUT_PATH $NUM_WORKERS

