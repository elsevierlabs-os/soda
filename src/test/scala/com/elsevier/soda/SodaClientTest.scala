package com.elsevier.soda

import org.junit.Test
import scala.io.Source
import java.io.File
import scala.util.parsing.json.JSON
import org.junit.Assert

class SodaClientTest {

    val sodaClient = new SodaClient()
    val props = SodaUtils.props
    val sodaServerHost = props("LB_HOSTNAME")
    
    ////////// json parsing tests ///////
    
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

    ///////////// client-server tests ///////////
    
    @Test
    def testIndex(): Unit = {
        val response = sodaClient.get(
            "http://%s:8080/soda/index.json".format(sodaServerHost))
        Console.println("index (unparsed) = " + response)
    }
    
    @Test
    def testListDicts(): Unit = {
        val response = sodaClient.get(
            "http://%s:8080/soda/dicts.json".format(sodaServerHost))
        Console.println("dicts (unparsed) = " + response)
    }
    
//    @Test
    def testDelete(): Unit = {
        val json = """{"lexicon" : "countries"}"""
        val response = sodaClient.post(
            "http://%s:8080/soda/delete.json".format(sodaServerHost), 
            json)
        Console.println("delete (unparsed): " + response)
    }
    
//    @Test
    def testAdd(): Unit = {
        testDelete()
        val lexicon = "countries"
        Source.fromFile(new File("src/test/resources/countries.csv")).getLines
            .foreach(line => {
                val cols = line.split(",")
                val id = "http://www.geonames.org/" + cols(0)
                val names = cols.toList
                val json = sodaClient.jsonBuild(Map(
                    "id" -> id, "names" -> names, "lexicon" -> lexicon, 
                    "commit" -> false))
                val response = sodaClient.post(
                    "http://%s:8080/soda/add.json".format(sodaServerHost), json)
                Console.println("add (unparsed): " + response)
            })
        val cjson = sodaClient.jsonBuild(Map(
            "lexicon" -> lexicon, "commit" -> true))
        val response = sodaClient.post(
            "http://%s:8080/soda/add.json".format(sodaServerHost), cjson)
        Console.println("add/commit (unparsed): " + response)
    }
    
    @Test
    def testLookup(): Unit = {
        val req = sodaClient.jsonBuild(Map(
            "id" -> "http://www.geonames.org/AND", 
            "lexicon" -> "countries"))
        val resp = sodaClient.post(
            "http://%s:8080/soda/lookup.json".format(sodaServerHost), req)
        Console.println("lookup (unparsed): " + resp)
        val data = sodaClient.jsonParse(resp)
        Console.println("lookup (parsed): " + data)
    }
    
    @Test
    def testAnnotate(): Unit = {
        val req = sodaClient.jsonBuild(Map(
            "lexicon" -> "countries",
            "text" -> "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China",
            "matching" -> "exact"))
        val resp = sodaClient.post("http://%s:8080/soda/annot.json".format(sodaServerHost), 
            req)
        Console.println("annotate (unparsed): " + resp)
        val data = sodaClient.jsonParseList(resp)
        Console.println("annotate (parsed): " + data)
    }
    
    @Test
    def testCoverage(): Unit = {
        val req = sodaClient.jsonBuild(Map(
            "text" -> "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China"
        ))
        val resp = sodaClient.post("http://%s:8080/soda/coverage.json".format(sodaServerHost), 
            req)
        Console.println("coverage (unparsed): " + resp)
        val data = sodaClient.jsonParseList(resp)
        Console.println("coverage (parsed): " + data)
    }
    
    @Test
    def testRegex(): Unit = {
        val patterns = Map("parenthesized" -> "\\(.*?\\)")
        val text = Source.fromFile(new File("src/test/resources/sildenafil.txt"))
            .getLines.mkString
        val req = sodaClient.jsonBuild(Map(
            "patterns" -> patterns,
            "text" -> text,
            "matching" -> "regex"
        ))
        val resp = sodaClient.post("http://%s:8080/soda/regex.json".format(sodaServerHost), req)
        Console.println("regex (unparsed): " + resp)
        val data = sodaClient.jsonParseList(resp)
        Console.println("regex (parsed): " + data)
    }
}
