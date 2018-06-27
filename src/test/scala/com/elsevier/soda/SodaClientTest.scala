package com.elsevier.soda

import com.elsevier.soda.messages._
import org.junit.{Assert, FixMethodOrder, Test}
import org.junit.runners.MethodSorters

import scala.io.Source

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SodaClientTest {

    val sodaClient = new SodaClient("http://localhost:8080")

    val lexiconName = "test_countries-2"
    val lookupId = "http://test-countries-2.com/ABW"
    val matchings = List("exact", "lower", "stop", "stem1", "stem2", "stem3")
    val text = "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China"

    @Test
    def test_001_index(): Unit = {
        val indexResponse = sodaClient.index()
        Assert.assertEquals("ok", indexResponse.status)
    }

    @Test
    def test_002_add(): Unit = {
        var numLoaded = 0
        Source.fromFile("src/main/resources/test-countries.tsv")
            .getLines()
            .foreach(line => {
                val Array(id, syns) = line.split("\t")
                val idModified = id.replace("test-countries", "test-countries-2")
                val names = syns.split("\\|").toArray
                val commit = (numLoaded % 100 == 0)
                val addResponse = sodaClient.add(lexiconName, idModified, names, commit)
                Assert.assertEquals("ok", addResponse.status)
                numLoaded += 1
            })
        val finalResponse = sodaClient.add(lexiconName, null, null, true)
        Assert.assertEquals("ok", finalResponse.status)
    }

    @Test
    def test_003_dicts(): Unit = {
        val dictResponse = sodaClient.dicts()
        Assert.assertEquals("ok", dictResponse.status)
        Assert.assertEquals(1, dictResponse.lexicons.filter(lc => lc.lexicon.equals(lexiconName)).size)
    }

    @Test
    def test_004_annot(): Unit = {
        matchings.foreach(matching => {
            val annotResponse = sodaClient.annot(lexiconName, text, matching)
            Assert.assertEquals("ok", annotResponse.status)
            Assert.assertTrue(annotResponse.annotations.size > 0)
        })
    }

    @Test
    def test_005_coverage(): Unit = {
        matchings.foreach(matching => {
            val coverageResponse = sodaClient.coverage(text, matching)
            Assert.assertEquals("ok", coverageResponse.status)
            Assert.assertEquals(1, coverageResponse.lexicons.filter(lc => lc.lexicon.equals(lexiconName)).size)
        })
    }

    @Test
    def test_006_lookup(): Unit = {
        val lookupResponse = sodaClient.lookup(lexiconName, lookupId)
        Assert.assertEquals("ok", lookupResponse.status)
        Assert.assertEquals(1, lookupResponse.entries.size)
    }

    @Test
    def test_007_delete(): Unit = {
        val deleteResponseOne = sodaClient.delete(lexiconName, lookupId)
        Assert.assertEquals("ok", deleteResponseOne.status)
        val deleteResponse = sodaClient.delete(lexiconName, "*")
        Assert.assertEquals("ok", deleteResponse.status)
    }
}
