curl -X POST -H 'Content-type:application/json' http://localhost:8983/solr/sodaindex/config -d '{
  "add-requesthandler" : {
    "name": "/tag",
    "class":"org.opensextant.solrtexttagger.TaggerRequestHandler",
    "defaults":{ "field" : "tagname_exact" }
  }
}'
