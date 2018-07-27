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
  - [Automatic Startup and Shutdown](#automatic-startup-and-shutdown)
- [Loading a Dictionary](#loading-a-dictionary)
- [Running SoDA local tests](#running-soda-local-tests)

----

This document lists out the steps needed to setup a SoDA server in the AWS cloud and visible to your Databricks cluster.

### Hardware and OS

The Hardware used for a single SoDA server box is a AWS EC2 m5.xlarge instance (16 GB RAM, 60 GB SSD, 4vCPU). Optionally, you can also select a different class of machine that has similar RAM and CPU, and attach EBS storage as described in [Making an Amazon EBS Volume Available for Use](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ebs-using-volumes.html). We use Ubuntu Server 16.04, please make the necessary mental adjustments for other Linux flavors such as Amazon Linux.

The following ports need to be open on the server.

* port 22 (ssh)
* port 80 and 443 (http, https)
* port 8080 (soda)
* port 8983 (solr)

All software is installed under the home directory of user `ubuntu`. 

_**Optional: if you used an EBS volume for your storage**_

If you used an external EBS volume, you would link the mount point to a directory under the home directory and that would become your installation directory. For example:

    $ cd /mnt
    $ mkdir ebs
    $ sudo chown -R ubuntu:ubuntu ebs
    $ cd
    $ ln -s /mnt/ebs .
    $ cd ebs

----

### Databricks Environment

_**This is optional, if you use Databricks notebooks to access SoDA for annotations.**_

The server needs to be accessible from within the Databricks notebook environment, so it is required to have a "private" IP address that is visible to the Databricks cluster. In addition, for maintenance work on the server, it has a "public" IP address into which one can ssh in with a PEM file.

----

### Software Installation

We will need to SSH into the SoDA AWS box using its public IP (or private IP in case you use a VPC). You can log into the newly provisioned box as follows:

    laptop$ ssh -i /path/to/your/pem/file ubuntu@public_or_private_ip

#### Install Base Software

Once logged in, install git (to download various softwares from github), Java 8, latest maven (to build SolrTextTagger), Scala and sbt (to build soda).

Install git, used to download various softwares (SoDA and SolrTextTagger) from github.

    $ sudo apt-get update
    $ sudo apt-get install git-core

Install the Oracle 8 JDK. The JDK provides the JVM which will run Solr as well as the webserver that SoDA will run inside of. In addition, the Scala compiler and runtime will also depend on the JDK. Here are the sequence of commands you want to do this. More detailed [instructions from DigitalOcean](https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-get-on-ubuntu-16-04) are available here. 

    $ sudo add-apt-repository ppa:webupd8team/java
    $ sudo apt-get update
    $ sudo apt-get install oracle-java8-installer
    $ sudo update-alternatives --config java  # select openjdk if not selected

Finally, set the `JAVA\_HOME="/usr/lib/jvm/java-8-oracle"` in your ${HOME}/.profile file and source it.

Install maven using the following commands. Maven will be used to build SolrTextTagger.

    $ sudo apt-get install maven

Install Scala. The current version is 2.12.6, and will be used to compile the SoDA application. [Detailed instructions](https://gist.github.com/Frozenfire92/3627e38dc47ca581d6d024c14c1cf4a9) here.

    $ sudo apt-get remove scala-library scala
    $ sudo wget http://scala-lang.org/files/archive/scala-2.12.1.deb
    $ sudo dpkg -i scala-2.12.1.deb
    $ sudo apt-get update
    $ sudo apt-get install scala

Install SBT (Scala Build Tool). SBT is used to build the SoDA application. [Detaild instructions](https://gist.github.com/Frozenfire92/3627e38dc47ca581d6d024c14c1cf4a9) from the same web page as for Scala.

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

Install Apache Solr. The latest version at the time of release was Solr 7.4.0.

    $ curl -O http://apache.mirrors.pair.com/lucene/solr/7.4.0/solr-7.4.0.tgz
    $ tar xvzf solr-7.4.0.tar.gz
    $ cd solr-7.4.0

Install the SolrTextTagger SNAPSHOT JAR file into Solr's lib directory.

    $ mkdir -p server/solr/lib
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

We can use either Apache Tomcat or Jetty as our web container. SoDA v.2 has been tested using Apache Tomcat 8.5.32 and Jetty 9.4.11.v20180605.

To download and expand Tomcat, run these commands:

    $ wget http://download.nextag.com/apache/tomcat/tomcat-8/v8.5.32/bin/apache-tomcat-8.5.32.tar.gz
    $ tar xvzf apache-tomcat-8.5.32.tar.gz

To download and expand Jetty, run these commands:

    $ wget https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-distribution/9.4.11.v20180605/jetty-distribution-9.4.11.v20180605.tar.gz
    $ tar xvzf jetty-distribution-9.4.11.v20180605.tar.gz

Deploy the WAR file `target/scala-2.12/soda_2.12-2.0.war` generated earlier using `sbt package`, to the container webapps directory as `soda.war`.

    $ cp target/scala-2.12/soda_2.12-2.0.war ${TOMCAT_HOME}/webapps/soda.war  # for tomcat
    $ cp target/scala-2.12/soda_2.12-2.0.war ${JETTY_HOME}/webapps/soda.war  # for jetty

Restart the container.

    $ cd ${TOMCAT_HOME}; bin/shutdown.sh; bin/startup.sh  # for tomcat
    $ cd ${JETTY_HOME}; bin/jetty.sh stop; bin/jetty.sh start  # for jetty


#### Verify Installation

You should be able to hit the SoDA index page at http://public\_ip:8080/soda/index.json, and it should return a JSON response saying "status": "ok" and the Solr version being used in the backend.


#### Automatic Startup and Shutdown

We leverage the Linux service management daemon systemd to automatically start and stop Solr and the SoDA container services after and before the server shuts down respectively. Support for systemd is available on Ubuntu 16.04 (the OS used in this guide), as well as AWS Linux, RHEL Centos 7.x and above, Debian 8 and above, Fedora, etc. The service makes assumptions about the location of the custom start and stop scripts (under `${SODA_HOME}/src/main/scripts`), the locations of the Solr installation (`${HOME}/solr-${version}`) and the web container (`${HOME}/apache-tomcat-${version}` or `${HOME}/jetty-distribution-${version}`), as well as the username (ubuntu) running the scripts. These values are baked inside the service description `soda.services` and the start and stop scripts `start_app.sh` and `stop_app.sh`, all under the `src/main/scripts` subdirectory. These assumptions are in line with the instructions so far, but it is likely that versions might have changed, so please make updates to these files as necessary for your installation.

Here are the sequence of commands that are needed to allow the services to be started up and shut down automatically. First (after checking the contents), we will deploy our service definition to where systemd expects to find it.

    $ cd ${SODA_HOME}/src/main/scripts
    $ sudo cp soda.service /etc/systemd/system/

Next we do a quick dry run to see if our scripts will run properly using systemd. This will call the `start_app.sh` script, which will first start Solr and then the SoDA web container (in my case Apache Tomcat). The output on the console should be whatever you expect to see when you run these start commands manually. Next we just run a `curl` command to verify that SoDA is accepting requests.

    $ sudo systemctl start soda
    $ curl http://localhost:8080/soda/index.json

Finally, we enable the service so it will run automatically whenever the server starts up, and stop the service with systemd, thereby also verifying that the `stop_app.sh` script works as intended.

    $ sudo systemctl enable soda
    $ sudo systemctl stop soda

----

### Loading a Dictionary

You can load a dictionary from a formatted tab-separated file using the SodaBulkLoader class provided with SoDA. Currently there is only a single main class, so SBT will not prompt for the class name to run. If prompted, choose the SodaBulkLoader class to run.

    $ sbt
    sbt> run ${lexicon_name} ${path_to_input_file} number_of_workers ${delete_lexicon}

Where the `lexicon_name` is the name of the dictionary the entries are to be loaded into, and the `path_to_input_file` represents the full path to the tab-separated data file. The `number_of_workers` value is the number of parallel workers that will be created to do the inserts and should be equal or less than the number of CPUs on the SoDA/Solr box. The `delete_lexicon` parameter can be set to either true or false, if true, it will first delete the named lexicon from the index before loading.

The file must have the following format:

    id {TAB} primary-name {PIPE} alt-name-1 {PIPE} ... {PIPE} alt-name-n

where {TAB} and {PIPE} represent the tab and pipe characters respectively.

The id field must be unique across lexicons. It is recommended that the id value be structured as a URI that incorporates the lexicon name in it.

If you prefer, there is also a script src/main/scripts/bulk\_load.sh which can be called as shown below. Note, however, that the classpath is based on a working sbt setup, with paths to JAR files pointing to the underlying ivy2 cache under the ${HOME} directory. If you have a different repository structure for your JAR files, you will very likely need to customize this script.

    $ cd src/main/scripts
    $ ./bulk_load.sh ${lexicon_name} ${path_to_input_file} ${num_workers} ${delete_lexicon}


----

### Running SoDA local tests

Scala tests can be run using SBT and Python sodaclient tests can be run using nose. Note that you need a running Solr instance and a SoDA instance for the tests to run successfully.

Bring up Solr if not already running.

    $ cd ${SOLR_HOME}
    $ bin/solr start

If SoDA is running inside a Tomcat or Jetty container, shut that down. The unit tests are designed to run against an embedded version of Jetty, where the application context is different from that in an external Tomcat or Jetty container.

    $ cd ${TOMCAT_HOME}; bin/shutdown.sh  # for Tomcat
    $ cd ${JETTY_HOME}; bin/jetty.sh stop  # for Jetty

Bring up SoDA in sbt using sbt's built-in Jetty server.

    $ cd ${SODA_HOME}
    $ sbt
    sbt> jetty:start

Run the Scala unit tests within sbt as follows (or on a different terminal using `sbt test`).

    sbt> test

Summary information about the number of tests being run and run successfully will be printed on the console. It is expected that all tests pass.

Next on a different terminal, navigate to the `src/main/python` subdirectory.

    $ cd ${SODA_HOME}/src/main/python
    $ nosetests sodaclient_test.py

Summary information about the number of tests being run and run successfully is printed on the console. As before, the expectation is that all tests pass.

Finally, you can turn off the built-in Jetty server and exit sbt.

    sbt> jetty:stop
    sbt> exit

Shut down Solr if not already shut down.

    $ cd ${SOLR_HOME}
    $ bin/solr stop

