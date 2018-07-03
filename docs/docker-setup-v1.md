# Running SoDA from Docker

Navdeep Sharma (navd) has created a Docker image for SoDA, with SoDA and Solr pre-installed. With this approach, all you need is a Docker installation. Instructions for using the Docker image follows.

Install Docker, if you already doesnt have on your machine. You can find [Docker Installation Instructions](https://docs.docker.com/engine/installation/) on their web page here.

Run following command to start dockerised version of SoDA:

`docker run -p 8983:8983 -p 8080:8080 -p 80:80 -p 443:443 navdeepsharma/soda`

This will create and deploy a container with SoDA, SolrTextTagger and Solr running inside.  

To change the configuration, get the ssh of docker container `docker exec -it container_Id /bin/bash`, edit `/home/soda/src/main/resources/soda.properties` file and restart the container.

Access Solr dashboard on [http://localhost:8983/solr/](http://localhost:8983/solr/) and SoDA dashboard on [http://localhost:8080/soda/index.jsp](http://localhost:8080/soda/index.jsp).


