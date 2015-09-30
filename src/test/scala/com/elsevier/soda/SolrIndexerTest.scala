package com.elsevier.soda

import java.io.File

import scala.io.Source

import org.junit.Test
import org.springframework.stereotype.Service

class SolrIndexerTest {

    @Test
    def testVerifyFindUSA(): Unit = {
        val testString = "Division of Respiratory Disease Studies, National Institute for Occupational Safety and Health, Morgantown, WV USA"
        val testString2 = "Academic Department of Obstetrics and Gynaecology, 3rd Floor, Lanesborough Wing, St. George's Hospital Medical School, Cranmer Terrace, London, SW17 0RE, UK"
        val sodaService = new SodaService()
        val annots = sodaService.annotate(testString2, "country", "exact")
        Console.println("# of annots: %d".format(annots.size))
        annots.foreach(Console.println(_))
    }
    
    @Test
    def testIndexLine(): Unit = {
        val indexer = new SolrIndexer("http://localhost:8983/solr/texttagger", 
            null, null, null, null)
        indexer.deleteLexicon("country", true)
        val lines = Source.fromFile(new File("data/countries.csv")).getLines
        lines.foreach(line => {
            val cols = line.split(",")
            val id = "http://www.geonames.org/" + cols(0)
            val names = cols.toList
            val lexicon = "countries"
            indexer.indexLine(id, names, lexicon, false)
        })
        indexer.indexLine(null, null, "countries", true)
    }
}
