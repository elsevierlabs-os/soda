# -*- coding: utf-8 -*-
from __future__ import division, print_function
import json
import requests

#lexicon = "countries"
#text = "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China"

lexicon = "emmet"
text = "This group of diseases is distinguished from RA by the absence of rheumatoid factor, or seronegativity. Among the diseases included in this category are ankylosing spondylitis (AS), reactive arthritis (formerly Reiter's syndrome), psoriatic arthritis (PsA), and arthritis associated with inflammatory bowel disease or enteropathic arthritis. The common articular features of this group of disorders are sacroiliitis, spondylitis, seronegative polyarthritis, and dactylitis."
#text = "This group of diseases"

params = {
    "lexicon": lexicon,
    "text": text,
    "matching" : "exact"
}
req = json.dumps(params)
resp = requests.post("http://localhost:8080/soda/annot.json", data=req)
#print(resp.text)
for rec in json.loads(resp.text):
    begin, end = rec["begin"], rec["end"]
    print(rec["id"], rec["coveredText"], begin, end, text[begin:end])
