##SoDA Installation and Configuration

###Table of Contents###

- [Hardware and OS](#hardware-and-os)
- [Databricks Environment](#databricks-environment)
- [Software Installation](#software-installation)
	- [Install Base Software](#install-base-software)
	- [Build SolrTextTagger](#build-solrtexttagger)
	- [Install Solr](#install-solr)
	- [Deploy SolrTextTagger JAR](#deploy-solrtexttagger-jar)
	- [Install Jetty](#install-jetty)
	- [Configure SoDA](#configure-soda)
	- [Build SoDA and Deploy](#build-soda-and-deploy)

_(generated with [DocToc](http://doctoc.herokuapp.com/))_

This document lists out the steps needed to setup a SoDA server in the AWS cloud and visible to your Databricks cluster.

###Hardware and OS

The Hardware used for a single SoDA server box is a AWS EC2 r3.large instance (15.5GB RAM, 32 GB SSD, 2vCPU). In addition, we have an additional 25GB SSD disk to hold the index and software. The server was running Amazon Linux (Centos/Redhat based distribution), commands are based on this flavor, please make the necessary mental adjustments for other Linux flavors such as Ubuntu.

The following ports need to be open on the server.

* port 22 (ssh)
* port 80 and 443 (http, https)
* port 8080 (soda)
* port 8983 (solr)

Additional hardware will be provided as an EBS volume. Follow instructions on the [Making an Amazon EBS Volume Available for Use](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ebs-using-volumes.html) page to install and mount it as a filesystem on your server. Once the EBS volume is built, make the space available to the ec2-user like so:

    $ cd /media/ephemeral
    $ sudo mkdir home
    $ sudo chown -R ec2-user:ec2-user home
    $ cd
    $ ln -s /media/ephemeral/home home
    $ cd home


###Databricks Environment

The server needs to be accessible from within the Databricks notebook environment, so it is required to have a "private" IP address that is visible to the Databricks cluster. In addition, for maintenance work on the server, it has a "public" IP address into which one can ssh in with a PEM file.

###Software Installation

For all the installation work that follows, we will use the public IP. You can log into the newly provisioned box as follows:

    laptop$ ssh -i ~/.ssh/solr-databricks.pem ec2-user@public_ip

####Install Base Software

Once in, install git (to download various softwares from github), Java 8 (the box comes with Java 7 but I wanted to run the latest), maven (to build SolrTextTagger), Scala and sbt (to build soda).

    $ sudo yum install git

    $ # download Java 8 RPM from Oracle onto laptop and scp it here
    $ sudo rpm -ivh jdk-8u45-linux-x64.rpm

    $ curl -O ftp://maven.apache.org/.../apache-maven-3.3.3-bin.tar.gz
    $ tar xvf apache-maven-3.3.3-bin.tar.gz

    $ curl -O http://www.scala-lang.org/.../scala-2.10.4.tgz
    $ tar xvf scala-2.10.4

    $ sudo yum install sbt

After this, you should also set up the following environment variables in your .bash_profile file.

    # Java
    export JAVA_HOME=/usr/java/jdk1.8.0_45

    # Maven
    export PATH=$HOME/home/apache-maven-3.3.3/bin:$PATH
    export MAVEN_OPTS="-Xms256m -Xmx512m"

    # Scala
    export SCALA_HOME=$HOME/home/scala-2.10.4
    export PATH=$SCALA_HOME/bin:$PATH

Finally, source the .bash_profile file so the new environment variables become active.

    $ . ~/.bash_profile

####Build SolrTextTagger

Download SolrTextTagger and build it.

    $ git clone https://github.com/OpenSextant/SolrTextTagger.git
    $ cd SolrTextTagger

We have made a small change locally to prevent SolrTextTagger from throwing an exception when the input text creates empty tokens during Tokenization. This is a consequence of our customized analyzer chain for SolrTextTagger "tag" fieldTypes. It was not submitted back because this looked like it was caused by our input not being validated rather than a bug in SolrTextTagger.

    $ patch -p1 < $SODA_HOME/docs/SolrTextTagger_patch.txt

Finally run the unit tests and build the package for deployment.

    $ mvn test
    $ mvn package

This will create a solr-text-tagger-2.1-SNAPSHOT.jar file which you will need to install into the Solr installation that we will set up next.

####Install Solr 

First step is to download and expand Solr. __Please note__ that SolrTextTagger is built with Solr-5.0.0 and there is a change in a Lucene method for the Terms object in the latest version (5.2.0) which will cause it to throw an exception. So make sure you use Solr-5.0.0 as well.

    $ curl -O http://solr.apache.com/.../solr-5.0.0.tgz
    $ tar xvzf solr-5.0.0.tgz
    $ cd solr-5.0.0

We then setup our index using an example as our template. We could (and probably should) do this differently in real-life, but Solr 5 was new to me and I was not very sure of the directory layout so decided to use the example to guide me.

    $ bin/solr -e techproducts
    $ bin/solr stop
    
Now we customize the techproducts example collection to make it our own.

    $ cd examples
    $ mv techproducts texttagger
    $ cd texttagger/solr
    $ mv techproducts texttagger
    $ cd texttagger
    $ vim core.properties # replace techproducts with texttagger

####Deploy SolrTextTagger JAR

SolrTextTagger is a Solr plugin that provides a custom request handler for text tagging. In order to enable this functionality in Solr, we need to put the JAR file in the Solr extensions classpath, and make configuration changes to declare new field types and analyzer chains that are specific to SolrTextTagger.

    $ mkdir lib
    $ cp ~/home/SolrTextTagger/target/solr-text-tagger-2.1-SNAPSHOT.jar lib/
    $ cd conf
    $ vim schema.xml and add the schema.xml snippet described below.
    $ vim solrconfig.xml and add the solrconfig.xml snippet described below.

You also need to increase the JVM Heap size for Solr since SolrTextTagger uses a memory based FST. Since Lucene uses the operating system memory, a balance must be struck between large JVM size and not starving the operating system. In my case, based on the [Use Lucenes MMapDirectory on 64 bit platforms, please!](http://blog.thetaphi.de/2012/07/use-lucenes-mmapdirectory-on-64bit.html) blog post by Uwe Schindler, I set the Solr heap size to 4 GB for my box with 15.5GB RAM and am getting adequate performance.

    $ vim bin/solr.in.sh
    SOLR_JAVA_MEM="-Xms4096m -Xmx4096m"

The schema.xml needs to get the new field type supported by SolrTextTagger and the field names that we will use for this application. Note that this chain is slightly modified from the one suggested in the SolrTextTagger site because we want to eliminate false positive matches caused by too much removal of "unnecessary" word elements.

    <fieldType name="tag" class="solr.TextField" 
               positionIncrementGap="100" postingsFormat="Memory" 
               omitTermFreqAndPositions="true" omitNorms="true">
        <analyzer type="index">
            <tokenizer class="solr.StandardTokenizerFactory"/>
            <filter class="solr.ASCIIFoldingFilterFactory"/>
            <filter class="solr.EnglishPossessiveFilterFactory" />
            <filter class="org.opensextant.solrtexttagger.ConcatenateFilterFactory" />
        </analyzer>
        <analyzer type="query">
            <tokenizer class="solr.StandardTokenizerFactory"/>
            <filter class="solr.ASCIIFoldingFilterFactory"/>
            <filter class="solr.EnglishPossessiveFilterFactory" />
        </analyzer>
    </fieldType>

    <field name="tagtype" type="string" indexed="true" stored="true" multiValued="true"/>
    <field name="tagsubtype" type="string" indexed="true" stored="true"/>
    <field name="tagname_str" type="string" indexed="true" stored="true" multiValued="true"/>
    <field name="tagname_nrm" type="string" indexed="true" stored="true" multiValued="true"/>
    <field name="tagname_srt" type="string" indexed="true" stored="true" multiValued="true"/>
    <field name="tagname_stm" type="string" indexed="true" stored="true" multiValued="true"/>
    <field name="tagname_stt" type="tag" indexed="true" stored="true" multiValued="true"/>

For solrconfig.xml, we need to add the following snippet to describe to Solr the custom SolrTextTagger service.

    <requestHandler name="/tag" 
                    class="org.opensextant.solrtexttagger.TaggerRequestHandler">
        <lst name="defaults">
            <str name="field">tagname_stt</str>
            <str name="fq">NOT name:(of the)</str><!-- filter out -->
        </lst>
    </requestHandler>

We then restart Solr (this time using the location of the server). The admin console at [http://public_ip:8983/solr] will give errors if there was a mistake in the above, otherwise we should be able to query the texttagger collection (the data is still from techproducts but this is just a sanity check).

    $ bin/solr -s example/texttagger/solr

When we need to stop Solr, we can just do:

    $ bin/solr stop

####Install Jetty

Finally we install the Jetty webserver. Similar to Maven and Scala, all we need to do is download a tarball that works with Java 8 (Jetty 9.2.11 in our case) and expand it into our local directory.

    $ curl -O http://download.eclipse.org/.../jetty-distribution-9.2.11.tar.gz
    $ tar xvzf jetty-distribution-9.2.11.tar.gz

To start and stop Jetty, we use the following commands. Jetty will start up on port 8080 on the machine.

    $ # renaming this for convenience
    $ mv jetty-distribution-9.2.11 jetty-9.2.11
    $ cd jetty-9.2.11
    $ bin/jetty.sh start
    $ bin/jetty.sh stop

####Configure SoDA

All configuration is done via the soda.properties file in src/main/resources. A template file is provided with the relevant properties to set and their descriptions (in comments) at [soda.properties.template](https://github.com/elsevierlabs-os/soda/blob/master/src/main/resources/soda.properties.template).

If you have a single Solr instance and are installing SoDA on the same box as Solr, then you can just copy the soda.properties.template to soda.properties. Otherwise, follow the directions in the soda.properties.template file to set the properties.

####Build SoDA and Deploy

Finally, we build our SoDA WAR file. This is done as follows:

    $ cd soda
    $ # build the WAR file in target/scala-2.10/soda-1.0-SNAPSHOT.war
    $ sbt package
    $ cp target/scala-2.10/soda-1.0-SNAPSHOT.war ../jetty-9.2.11/webapps/

and restart Jetty. You should be able to see the SoDA index page at http://public_ip:8080/soda/index.html.

