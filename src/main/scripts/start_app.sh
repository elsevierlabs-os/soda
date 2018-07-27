#!/bin/bash
# Starts Solr and SoDA container services
SOLR_HOME=${HOME}/solr-7.4.0
APPSERVER_HOME=${HOME}/apache-tomcat-8.5.32

echo "starting solr..."
cd ${SOLR_HOME} && bin/solr start

if [[ ${APPSERVER_HOME} = *"apache"* ]]; then
    echo "starting tomcat"
    cd ${APPSERVER_HOME} && bin/startup.sh
elif [[ ${APPSERVER_HOME} = *"jetty"* ]]; then
    echo "starting jetty"
    cd ${APPSERVER_HOME} && bin/jetty.sh start
else
    echo "Only Jetty and Tomcat supported for now"
    exit -1
fi
