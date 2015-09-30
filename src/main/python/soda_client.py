import json
import requests

# index
resp = requests.get("http://localhost:8080/soda/index.json")
print json.loads(resp.text)

# list
resp = requests.get("http://localhost:8080/soda/dicts.json")
print json.loads(resp.text)

# annotate
params = {
    "lexicon" : "countries",
    "text" : "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China",
    "matching" : "exact"
}
req = json.dumps(params)
resp = requests.post("http://localhost:8080/soda/annot.json", json=req)
print json.loads(resp.text)

# coverage
params = { "text" : "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China" }
req = json.dumps(params)
resp = requests.post("http://localhost:8080/soda/coverage.json", req)
print json.loads(resp.text)

