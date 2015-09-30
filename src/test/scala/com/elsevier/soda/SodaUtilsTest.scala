package com.elsevier.soda

import org.junit.Test
import scala.util.parsing.json.JSON
import scala.util.parsing.json.JSONObject
import org.junit.Assert

class SodaUtilsTest {

//    @Test
    def testJsonLoad(): Unit = {
       val s = """{"id": "http://foo/bar/1", "names": ["foo", "bar"], "lexicon": "test", "commit": false}"""
       val m = SodaUtils.jsonParse(s)
       Console.println("m=" + m)
       Assert.assertEquals(2, m("names").asInstanceOf[List[String]].size)
       Assert.assertTrue(m("lexicon").equals("test"))
    }
    
//    @Test
    def testRealJson(): Unit = {
//        val s = """{"id" : "http:\/\/www.geonames.org\/ZAF", "names" : List(ZAF, South Africa, Afrique du Sud), "lexicon" : "countries", "commit" : false}"""
        val s = """{"id" : "http://www.geonames.org/ZAF", "names" : ["ZAF", "South Africa", "Afrique du Sud"], "lexicon" : "countries", "commit" : false}"""
        val m = SodaUtils.jsonParse(s)
        Console.println(m)
    }
    
    @Test
    def testJsonDump(): Unit = {
        val m = Map("id" -> "http://foo/bar/1",
                    "names" -> List("foo", "bar"),
                    "lexicon" -> "test",
                    "commit" -> false)
        Console.println("m=" + m)
        val s = SodaUtils.jsonBuild(m)
        Console.println("s=" + s)
        val m2 = SodaUtils.jsonParse(s)
        Console.println("m2=" + m2)
        Assert.assertTrue(m("lexicon").equals(m2("lexicon")))
        Assert.assertTrue(m("names").equals(m2("names")))
    }
}
