# -*- coding: utf-8 -*-
from __future__ import division, print_function
import json
import requests
import urllib2
import urllib

solr_port = 80
lexicon = "emmet"
match_type = "x" # exact

#query = """+tagtype:emmet +tagsubtype:x +tagname_str:"reiter syndrome" """
query = "+id:http\:\/\/www.elsevier.com\/emmet\/concept\/9001551 +tagtype:emmet +tagsubtype:x"

params = {
    "q" : query,
    "wt": "json"
}
params_encoded = urllib.urlencode(params)
resp = requests.get("http://localhost:80/solr/texttagger/select?{:s}"
                    .format(params_encoded))
resp_json = json.loads(resp.text)
#print(resp_json)
for hit in resp_json["response"]["docs"]:
    print(hit["id"], hit["tagname_str"])

