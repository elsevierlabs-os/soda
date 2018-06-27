package com.elsevier.soda

import java.io.InputStream

import scala.io.Source
import org.apache.commons.io.IOUtils
import org.apache.commons.text.similarity.LevenshteinDistance

object SodaUtils extends java.io.Serializable {
    
    def props(): Map[String,String] = {
        var istream: InputStream = null
        try {
            istream = getClass.getClassLoader
                              .getResourceAsStream("soda.properties")
            Source.fromInputStream(istream)
                  .getLines()
                  .toList
                  .filter(line => (!line.startsWith("#") && line.trim.length > 0))
                  .map(line => {
                       val Array(key, value) = line.split("=")
                       (key, value)
                  })
                  .toMap
        } finally {
            IOUtils.closeQuietly(istream)
        }
    }

    // adapted from: https://stackoverflow.com/questions/955110/similarity-string-comparison-in-java
    def similarity(s1: String, s2: String): Double = {
        val lsPair = if (s1.length < s2.length) (s2, s1) else (s1, s2)
        val longerLength = lsPair._1.length.toDouble
        if (longerLength == 0) return 1.0D
        else {
            val metric = new LevenshteinDistance()
            val editDistance = metric.apply(lsPair._1, lsPair._2)
            (longerLength - editDistance) / longerLength
        }
    }

    def computeConfidence(coveredText: String, id: String, id2names: Map[String,List[String]],
                          matchType: String): Double = {
        if ("exact".equals(matchType))
            // if exact match, it must have matched one of the syns by definition,
            // so no need to actually compute the best value
            1.0D
        else {
            // all matchType != "exact" are lowercased before matching
            // other stemming and stopwording transformations take place inside index
            // but all of them require a lower case input
            val sims = id2names(id).map(name => {
                val sim = similarity(coveredText.toLowerCase, name.toLowerCase)
                (name, sim)
            })
            sims.sortWith((a, b) => a._2 > b._2)
                .head
                ._2
        }
    }

    lazy val luceneReservedChars = """+-&|!(){}[]^"~*?:\"""
        .toCharArray.toSet

    def escapeLucene(query: String): String = {
        query.toCharArray.map(c => if (luceneReservedChars.contains(c)) "\\" + c else c)
            .mkString
    }
}
