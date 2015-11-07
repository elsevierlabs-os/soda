package com.elsevier.soda

import org.junit.Test
import scala.io.Source
import java.io.File
import scala.util.parsing.json.JSON
import org.junit.Assert

class SodaClientTest {

    val sodaClient = new SodaClient()
    
//    @Test
    def testIndex(): Unit = {
        val response = sodaClient.get(
            "http://localhost:8080/soda/index.json")
        Console.println(response)
    }
    
//    @Test
    def testListDicts(): Unit = {
        val response = sodaClient.get(
            "http://localhost:8080/soda/dicts.json")
        Console.println(response)
    }
    
//    @Test
    def testDelete(): Unit = {
        val json = """{"lexicon" : "countries"}"""
        val response = sodaClient.post(
            "http://localhost:8080/soda/delete.json", json)
        Console.println(response)
    }
    
//    @Test
    def testAdd(): Unit = {
        testDelete()
        val lexicon = "countries"
        Source.fromFile(new File("data/countries.csv")).getLines
            .foreach(line => {
                val cols = line.split(",")
                val id = "http://www.geonames.org/" + cols(0)
                val names = cols.toList
                val json = sodaClient.jsonBuild(Map(
                    "id" -> id, "names" -> names, "lexicon" -> lexicon, 
                    "commit" -> false))
                val response = sodaClient.post(
                    "http://localhost:8080/soda/add.json", json)
                Console.println("save(" + id + "): " + response)
            })
        val cjson = sodaClient.jsonBuild(Map(
            "lexicon" -> lexicon, "commit" -> true))
        val response = sodaClient.post(
            "http://localhost:8080/soda/add.json", cjson)
        Console.println("commit: " + response)
    }
    
//    @Test
    def testLookup(): Unit = {
        val req = sodaClient.jsonBuild(Map(
            "id" -> "http://www.geonames.org/AND", 
            "lexicon" -> "countries"))
        val resp = sodaClient.post(
            "http://localhost:8080/soda/lookup.json", req)
        Console.println("response (unparsed): " + resp)
        val data = sodaClient.jsonParse(resp)
        Console.println("parsed: " + data)
    }
    
//    @Test
    def testAnnotate(): Unit = {
        val req = sodaClient.jsonBuild(Map(
            "lexicon" -> "countries",
            "text" -> "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China",
            "matching" -> "exact"))
        val resp = sodaClient.post("http://localhost:8080/soda/annot.json", req)
        Console.println("response (unparsed): " + resp)
        val data = sodaClient.jsonParseList(resp)
        Console.println("parsed: " + data)
    }
    
//    @Test
    def testCoverage(): Unit = {
        val req = sodaClient.jsonBuild(Map(
            "text" -> "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China"
        ))
        val resp = sodaClient.post("http://localhost:8080/soda/coverage.json", req)
        Console.println("response (unparsed): " + resp)
        val data = sodaClient.jsonParseList(resp)
        Console.println("parsed: " + data)
    }
    
    @Test
    def testJson1(): Unit = {
        val params = Map(
            "lexicon" -> "wikidata",
            "text" -> "This is the house that Jack built.",
            "matching" -> "exact"
        )
        val req = sodaClient.jsonBuild(params)
        Console.println("req:" + req)
        val parsedReq = sodaClient.jsonParse(req)
        Console.println("parsed:" + parsedReq)
    }
    
    @Test 
    def testJson2(): Unit = {
        val params = Map(
            "regex" -> Map(
                "doi" -> "some regex pattern",
                "nature" -> "some other regex pattern"
            ),
            "text" -> "This is the house that Jack built.",
            "mathching" -> "regex"
        )
        val req = sodaClient.jsonBuild(params)
        Console.println("req:" + req)
        val parsedReq = sodaClient.jsonParse(req)
        Console.println("parsed:" + parsedReq)
    }
    
    @Test
    def testJson3(): Unit = {
        val output = "[" + 
            List(sodaClient.jsonBuild(Map(
                "id" -> "http://foo.org/entity/1234",
                "begin" -> 30,
                "end" -> 33,
                "coveredText" -> "foo",
                "confidence" -> 1.0
            )),
            sodaClient.jsonBuild(Map(
                "id" -> "http://foo.org/entity/3456",
                "begin" -> 20,
                "end" -> 23,
                "coveredText" -> "bar",
                "confidence" -> 1.0
            ))).mkString(",") + "]"
        val results = sodaClient.jsonParseList(output)
        Console.println("results:" + results)
    }
}
