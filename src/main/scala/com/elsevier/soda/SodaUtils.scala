package com.elsevier.soda

import java.io.InputStream
import scala.collection.JavaConversions._
import scala.io.Source
import org.apache.commons.io.IOUtils
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.ArrayList
import org.apache.commons.lang3.text.translate.LookupTranslator
import java.util.regex.Pattern
import scala.util.parsing.json.JSON
import scala.util.parsing.json.JSONObject
import scala.util.parsing.json.JSONArray

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
    
    def stopwords(): Set[String] = {
        var istream: InputStream = null
        try {
            istream = getClass.getClassLoader
                              .getResourceAsStream("stopwords.txt")
            Source.fromInputStream(istream)
                  .getLines()
                  .toSet
        } finally {
            IOUtils.closeQuietly(istream)
        }
    }
    
    lazy val luceneReservedChars = """+-&|!(){}[]^"~*?:\"""
        .toCharArray.toSet

    def escapeLucene(query: String): String = {
        query.toCharArray.map(c => 
            if (luceneReservedChars.contains(c)) "\\" + c
            else c).mkString
    }
    
    val AbbrevPattern = Pattern.compile("[A-Z0-9\\p{Punct}]+")
    def isAbbreviation(s: String): Boolean = {
        val m = AbbrevPattern.matcher(s)
        m != null && m.matches()
    }
    
    def isStopword(s: String, stopwords: Set[String]): Boolean =
        stopwords.contains(s.toLowerCase())
    
    def isTooShort(s: String): Boolean = {
        if (s.isEmpty) true
        else s.replaceAll("\\p{Punct}", "").length() < 3
    }
    
    def stripEscapes(s: String): String = {
        val ss = s.replaceAll("\\\\", "")
        if (ss.startsWith("\"") && ss.endsWith("\"")) 
            ss.substring(1, ss.length() - 1)
        else ss
    }
        
    def OK(): String = """{"status": "ok"}"""
        
    def error(message: String): String = 
        """{"status": "error", "message": "%s"}""".format(message)

}
