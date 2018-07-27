#!/bin/bash
# Stops Solr and Soda container services
SOLR_HOME=${HOME}/solr-7.4.0
APPSERVER_HOME=${HOME}/apache-tomcat-8.5.32

if [[ ${APPSERVER_HOME} = *"apache"* ]]; then
    echo "stopping tomcat"
    cd ${APPSERVER_HOME} && bin/shutdown.sh
elif [[ ${APPSERVER_HOME} = *"jetty"* ]]; then
    echo "stopping jetty"
    cd ${APPSERVER_HOME} && bin/jetty.sh stop
else
    echo "Only Jetty and Tomcat supported for now"
    exit -1
fi

echo "stopping solr..."
cd ${SOLR_HOME} && bin/solr stop
