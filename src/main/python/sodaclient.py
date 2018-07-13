# -*- coding: utf-8 -*-
import json
import requests

class SodaClient(object):
    
    def __init__(self, url):
        self.url = url
        
    
    def index(self):
        resp = requests.get(self.url + "/index.json")
        index_resp = json.loads(resp.text)
        return index_resp
    
    
    def add(self, lexicon, id, names, commit):
        body = json.dumps({
            "lexicon": lexicon,
            "id": id,
            "names": names,
            "commit": commit
        })
        resp = requests.post(self.url + "/add.json", data=body)
        add_resp = json.loads(resp.text)
        return add_resp
        
    
    def delete(self, lexicon, id):
        body = json.dumps({
            "lexicon": lexicon,
            "id": id
        })
        resp = requests.post(self.url + "/delete.json", data=body)
        delete_resp = json.loads(resp.text)
        return delete_resp
    
    
    def annot(self, lexicon, text, matching):
        body = json.dumps({
            "lexicon": lexicon,
            "text" : text,
            "matching": matching
        })
        resp = requests.post(self.url + "/annot.json", data=body)
        annot_resp = json.loads(resp.text)
        return annot_resp
    
    
    def dicts(self):
        resp = requests.get(self.url + "/dicts.json")
        dicts_resp = json.loads(resp.text)
        return dicts_resp
    
    
    def coverage(self, text, matching):
        body = json.dumps({
            "text": text,
            "matching": matching
        })
        resp = requests.post(self.url + "/coverage.json", data=body)
        coverage_resp = json.loads(resp.text)
        return coverage_resp
    
    
    def lookup(self, lexicon, id):
        body = json.dumps({
            "lexicon": lexicon,
            "id": id
        })
        resp = requests.post(self.url + "/lookup.json", data=body)
        lookup_resp = json.loads(resp.text)
        return lookup_resp


    def rlookup(self, lexicon, phrase, matching):
        body = json.dumps({
            "lexicon": lexicon,
            "phrase": phrase,
            "matching": matching
        })
        resp = requests.post(self.url + "/rlookup.json", data=body)
        lookup_resp = json.loads(resp.text)
        return lookup_resp
