Install Docker, if you already doesn't have on your machine. 
Reference https://docs.docker.com/engine/installation/

Run following command to start dockerised version of soda:

`docker run -p 8983:8983 -p 8080:8080 -p 80:80 -p 443:443 navdeepsharma/soda`

This will create and deploy a container with SODA, SolrTextTagger and Solr running inside.  

To change the configuration, get the ssh of docker container `docker exec -it container_Id /bin/bash`, edit `/home/soda/src/main/resources/soda.properties` file and restart the container.

Access Solr Dashboard on http://localhost:8983/solr/
