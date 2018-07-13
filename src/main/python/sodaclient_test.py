# -*- coding: utf-8 -*-
import unittest
import sodaclient

SODA_URL = "http://localhost:8080"
LEXICON_NAME = "test_countries-3"

MATCHINGS = ["exact", "lower", "stop", "stem1", "stem2", "stem3"]
LOOKUP_ID = "http://test-countries-3.com/ABW"
TEXT = "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China"

PHRASE = "Emirates"
PHRASE_MATCHINGS = ["lsort", "s3sort"]
PHRASE_MATCHINGS.extend(MATCHINGS)

class SodaClientTest(unittest.TestCase):
    
    def test_001_index(self):
        soda_client = sodaclient.SodaClient(SODA_URL)
        index_resp = soda_client.index()
        self.assertEquals("ok", index_resp["status"])
    

    def test_002_add(self):
        soda_client = sodaclient.SodaClient(SODA_URL)
        fin = open("../resources/test-countries.tsv", "r")
        num_loaded = 0
        for line in fin:
            id, syns = line.strip().split("\t")
            names = syns.split("|")
            id = id.replace("test-countries", "test-countries-3")
            commit = num_loaded % 100 == 0
            add_resp = soda_client.add(LEXICON_NAME, id, names, commit)
            self.assertEquals("ok", add_resp["status"])
            num_loaded += 1
        add_resp = soda_client.add(LEXICON_NAME, None, None, True)
        self.assertEquals("ok", add_resp["status"])        
    
    
    def test_003_dicts(self):
        soda_client = sodaclient.SodaClient(SODA_URL)
        dict_resp = soda_client.dicts()
        self.assertEquals("ok", dict_resp["status"])

    
    def test_004_annot(self):
        soda_client = sodaclient.SodaClient(SODA_URL)
        for matching in MATCHINGS:
            annot_resp = soda_client.annot(LEXICON_NAME, TEXT, matching)
            self.assertEquals("ok", annot_resp["status"])

    
    def test_005_coverage(self):
        soda_client = sodaclient.SodaClient(SODA_URL)
        for matching in MATCHINGS:
            coverage_resp = soda_client.coverage(TEXT, matching)
            self.assertEquals("ok", coverage_resp["status"])

    
    def test_006_lookup(self):
        soda_client = sodaclient.SodaClient(SODA_URL)
        lookup_resp = soda_client.lookup(LEXICON_NAME, LOOKUP_ID)
        self.assertEquals("ok", lookup_resp["status"])
        self.assertEquals(1, len(lookup_resp["entries"]))


    def test_007_rlookup(self):
        soda_client = sodaclient.SodaClient(SODA_URL)
        for matching in PHRASE_MATCHINGS:
            rlookup_resp = soda_client.rlookup(LEXICON_NAME, PHRASE, matching)
            self.assertEquals("ok", rlookup_resp["status"])


    def test_008_delete(self):
        soda_client = sodaclient.SodaClient(SODA_URL)
        delete_resp_single = soda_client.delete(LEXICON_NAME, LOOKUP_ID)
        self.assertEquals("ok", delete_resp_single["status"])
        delete_resp = soda_client.delete(LEXICON_NAME, "*")
        self.assertEquals("ok", delete_resp["status"])



if __name__ == "__main__":
    unittest.main()
