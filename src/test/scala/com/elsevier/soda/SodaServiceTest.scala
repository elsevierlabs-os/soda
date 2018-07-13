package com.elsevier.soda

import com.elsevier.soda.messages._
import com.google.gson.Gson
import org.junit.runners.MethodSorters
import org.junit.{Assert, FixMethodOrder, Test}

import scala.io.Source

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SodaServiceTest {

    val sodaService = new SodaService()
    val gson = new Gson()

    val lexiconName = "test_countries"
    val lookupId = "http://test-countries.com/ABW"
    val matchings = List("exact", "lower", "stop", "stem1", "stem2", "stem3")
    val text = "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China"
    val phrase = "Emirates"
    val phraseMatchings = matchings ++ List("lsort", "s3sort")

    @Test
    def test_001_checkStatus(): Unit = {
        try {
            val response = sodaService.checkStatus()
            val indexResponse = gson.fromJson(response, classOf[IndexResponse])
            Assert.assertEquals("ok", indexResponse.status)
        } catch {
            case e: Exception => Assert.fail("Exception thrown during checkStatus")
        }
    }

    @Test
    def test_002_addEntry(): Unit = {
        var numLoaded = 0
        try {
            Source.fromFile("src/main/resources/test-countries.tsv")
                .getLines
                .foreach(line => {
                    val Array(id, syns) = line.split("\t")
                    val names = syns.split("\\|").toArray
                    val commit = numLoaded % 100 == 0
                    val addRequest = AddRequest(lexiconName, id, names, commit)
                    val response = sodaService.addEntry(gson.toJson(addRequest))
                    val addResponse = gson.fromJson(response, classOf[AddResponse])
                    Assert.assertEquals("ok", addResponse.status)
                    numLoaded += 1
                })
            val finalCommit = AddRequest(lexiconName, null, null, true)
            sodaService.addEntry(gson.toJson(finalCommit))
        } catch {
            case e: Exception => Assert.fail("Exception thrown during addEntry")
        }
    }

    @Test
    def test_003_listLexicons(): Unit = {
        val response = sodaService.listLexicons()
        val dictResponse = gson.fromJson(response, classOf[DictResponse])
        Assert.assertEquals("ok", dictResponse.status)
        Assert.assertEquals(1, dictResponse.lexicons
            .filter(lexiconCount => lexiconCount.lexicon.equals(lexiconName))
            .size)
        Assert.assertEquals(248, dictResponse.lexicons
            .filter(lexiconCount => lexiconCount.lexicon.equals(lexiconName))
            .head.count)
    }

    @Test
    def test_004_annotateText(): Unit = {
        matchings.foreach(matching => {
            val request = gson.toJson(AnnotRequest(lexiconName, text, matching))
            val response = sodaService.annotateText(request)
            val annotResponse = gson.fromJson(response, classOf[AnnotResponse])
            Assert.assertEquals("ok", annotResponse.status)
            Assert.assertTrue(annotResponse.annotations.size > 0)
        })
    }

    @Test
    def test_005_computeCoverage(): Unit = {
        matchings.foreach(matching => {
            val request = gson.toJson(CoverageRequest(text, matching))
            val response = sodaService.computeCoverage(request)
            val coverageResponse = gson.fromJson(response, classOf[CoverageResponse])
            Assert.assertEquals("ok", coverageResponse.status)
            Assert.assertEquals(1, coverageResponse.lexicons
                .filter(coverageCount => coverageCount.lexicon.equals(lexiconName))
                .size)
        })
    }

    @Test
    def test_006_lookupLexicon(): Unit = {
        val response = sodaService.lookupLexiconEntry(gson.toJson(LookupRequest(lexiconName, lookupId)))
        val lookupResponse = gson.fromJson(response, classOf[LookupResponse])
        Assert.assertEquals("ok", lookupResponse.status)
        Assert.assertEquals(1, lookupResponse.entries.size)
    }

    @Test
    def test_007_reverseLookupLexicon(): Unit = {
        phraseMatchings.foreach(matching => {
            val request = gson.toJson(ReverseLookupRequest(lexiconName, phrase, matching))
            val response = sodaService.reverseLookupPhrase(request)
            val reverseLookupResponse = gson.fromJson(response, classOf[ReverseLookupResponse])
            Assert.assertEquals("ok", reverseLookupResponse.status)
            Assert.assertEquals(1, reverseLookupResponse.entries.size)
        })
    }

    @Test
    def test_008_deleteEntryOrLexicon(): Unit = {
        val responseOne = sodaService.deleteEntryOrLexicon(gson.toJson(DeleteRequest(lexiconName, lookupId)))
        val deleteResponseOne = gson.fromJson(responseOne, classOf[DeleteResponse])
        Assert.assertEquals("ok", deleteResponseOne.status)
        val response = sodaService.deleteEntryOrLexicon(gson.toJson(DeleteRequest(lexiconName, "*")))
        val deleteResponse = gson.fromJson(response, classOf[DeleteResponse])
        Assert.assertEquals("ok", deleteResponse.status)
    }
}
