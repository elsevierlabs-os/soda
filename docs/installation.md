## SoDA Installation and Configuration


### Table of Contents

- [Hardware and OS](#hardware-and-os)
- [Databricks Environment](#databricks-environment)
- [Software Installation](#software-installation)
  - [Install Base Software](#install-base-software)
  - [Build SolrTextTagger](#build-solrtexttagger)
  - [Install and Configure SoDA](#install-and-configure-soda)
  - [Install and Configure Solr](#install-and-configure-solr)
  - [Install web server container](#install-web-server-container)
  - [Deploy SoDA WAR file](#deploy-soda-war-file)
  - [Verify Installation](#verify-installation)
- [Loading a Dictionary](#loading-a-dictionary)
- [Running SoDA local tests](#running-soda-local-tests)

----

This document lists out the steps needed to setup a SoDA server in the AWS cloud and visible to your Databricks cluster.

### Hardware and OS

The Hardware used for a single SoDA server box is a AWS EC2 r3.large instance (15.5GB RAM, 32 GB SSD, 2vCPU). In addition, we have an additional 25GB SSD disk to hold the index and software. We use Ubuntu Server 16.04, please make the necessary mental adjustments for other Linux flavors such as Amazon Linux.

The following ports need to be open on the server.

* port 22 (ssh)
* port 80 and 443 (http, https)
* port 8080 (soda)
* port 8983 (solr)

Additional hardware will be provided as an EBS volume. Follow instructions on the [Making an Amazon EBS Volume Available for Use](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ebs-using-volumes.html) page to install and mount it as a filesystem on your server. Once the EBS volume is built, make the space available to the ec2-user like so:

    $ cd /mnt
    $ mkdir ebs
    $ sudo chown -R ec2-user:ec2-user ebs
    $ cd
    $ ln -s /mnt/ebs .
    $ cd ebs

----

### Databricks Environment

_**This is optional, if you use Databricks notebooks to access SoDA for annotations.**_

The server needs to be accessible from within the Databricks notebook environment, so it is required to have a "private" IP address that is visible to the Databricks cluster. In addition, for maintenance work on the server, it has a "public" IP address into which one can ssh in with a PEM file.

----

### Software Installation

We will need to log into the SoDA AWS box using its public IP (or private IP in case you use a VPC). You can log into the newly provisioned box as follows:

    laptop$ ssh -i ~/.ssh/solr-databricks.pem ubuntu@public_ip

#### Install Base Software

Once logged in, install git (to download various softwares from github), Java 8, latest maven (to build SolrTextTagger), Scala and sbt (to build soda).

Install git, used to download various softwares (SoDA and SolrTextTagger) from github.

    $ sudo apt-get update
    $ sudo apt-get install git-core

Install the Oracle 8 JDK using these [instructions from DigitalOcean](https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-get-on-ubuntu-16-04). The JDK provides the JVM which will run Solr as well as the webserver that SoDA will run inside of. In addition, the Scala compiler and runtime will also depend on the JDK.

Install maven using the following commands. Maven will be used to build SolrTextTagger.

    $ sudo apt-get install maven

Install Scala. The current version is 2.12.6, and will be used to compile the SoDA application.

    $ sudo apt-get install scala

Install SBT (Scala Build Tool). SBT is used to build the SoDA application.

    $ echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
    $ sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
    $ sudo apt-get update
    $ sudo apt-get install sbt


#### Build SolrTextTagger

Download SolrTextTagger and build it.

    $ git clone https://github.com/OpenSextant/SolrTextTagger.git
    $ cd SolrTextTagger
    $ mvn test
    $ mvn package

This will create a solr-text-tagger-2.6-SNAPSHOT.jar file in the project's target folder, which you will need to install into the Solr installation that we will set up next.

#### Install and Configure SoDA

Download SoDA.

    $ git clone https://github.com/elsevierlabs-os/soda.git
    $ cd soda

All SoDA configuration parameters are read from src/main/resources/soda.properties. If SoDA is co-located with Solr, then you can just copy the soda.properties.template to soda.properties, otherwise modify the values as needed.

    $ cd src/main/resources
    $ cp soda.properties.template soda.properties
    $ cd -

Build SoDA. The build artifacts can be found in the target/scala-2.12 folder. The WAR file soda\_2.12-2.0.war is for the SoDA server, and the JAR file soda\_2.12-2.0.jar can be used if you want to incorporate the Scala SoDA client inside your application.

    $ sbt package

#### Install and Configure Solr 

Install Apache Solr. The latest version at the time of release was Solr 7.3.0.

    $ curl -O http://apache.mirrors.pair.com/lucene/solr/7.3.0/solr-7.3.0.tgz
    $ tar xvzf solr-7.3.0.tar.gz
    $ cd solr-7.3.0

Install the SolrTextTagger SNAPSHOT JAR file into Solr's lib directory.

    $ cp ../SolrTextTagger/target/solr-text-tagger-2.6-SNAPSHOT.jar server/solr/lib

Increase the Solr JVM heap size from the default to a more generous 4 GB.

    $ vim bin/solr.in.sh
    SOLR_JAVA_MEM="-Xms4096m -Xmx4096m"

Start the Solr server.

    $ bin/solr start

Create the sodaindex core. (Note: in order to remove the core, replace create with delete in the command below).

    $ bin/solr create -c sodaindex

Create the schema for the sodaindex core. This uses the Solr JSON API to set up the field type needed by SolrTextTagger and the fields that SoDA needs to store the dictionary entries in various stages of stemming. If Solr is going to be co-located on the same box as SoDA, then you can just run the schema.sh command packaged with the SoDA source. Otherwise, modify the hostname in the curl command.

    $ ${SODA_HOME}/src/main/scripts/update_schema.sh

Add the SolrTextTagger handler to the Solr configuration. As with the schema, we can use the Solr JSON API to do this with a curl command. No changes are needed if Solr and SoDA are colocated on the same machine, otherwise the hostname needs to be changed.

    $ ${SODA_HOME}/src/main/scripts/update_solrconfig.sh

Restart Solr to allow the schema and configuration changes to take effect.

    $ bon/solr restart

#### Install web server container

We can use either Apache Tomcat or Jetty as our web container.

    $ sudo apt-get install tomcat8
    $ sudo apt-get install jetty9

#### Deploy SoDA WAR file

We only describe the command to deploy to the Jetty container, but the command to deploy to Apache is similar.

    $ cp ${SOLR_HOME}/target/scala-2.12/soda_2.12-2.0.war ${JETTY_HOME}/webapps/soda.war
    $ ${JETTY_HOME}/bin/jetty.sh start

#### Verify Installation

You should be able to hit the SoDA index page at http://public\_ip:8080/soda/index.json, and it should return a JSON response saying "status": "ok" and the Solr version being used in the backend.

----

### Loading a Dictionary

You can load a dictionary from a formatted tab-separated file using the SodaBulkLoader class provided with SoDA. Currently there is only a single main class, so SBT will not prompt for the class name to run. If prompted, choose the SodaBulkLoader class to run.

    $ sbt
    sbt> run ${lexicon_name} ${path_to_input_file}

Where the lexicon\_name is the name of the dictionary the entries are to be loaded into, and the path\_to\_input\_file represents the full path to the tab-separated data file.

The file must have the following format:

    id {TAB} primary-name|alt-name-1|alt-name-2|...|alt-name-n

The id field has to be unique across lexicons. It is recommended that the id value be structured as a URI that incorporates the lexicon name in it.

----

### Running SoDA local tests

Scala tests can be run using SBT and Python sodaclient tests can be run using nose.

    $ cd ${SODA_HOME}
    $ sbt test
    $ cd src/main/python
    $ nosetests tests

