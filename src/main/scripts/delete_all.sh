curl "http://localhost:8983/solr/sodaindex/update?stream.body=<delete><query>*:*</query></delete>"
curl "http://localhost:8983/solr/sodaindex/update?stream.body=<commit/>"

