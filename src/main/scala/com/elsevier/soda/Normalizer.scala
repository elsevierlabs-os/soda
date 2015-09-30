package com.elsevier.soda

import java.util.regex.Pattern

import scala.collection.mutable.ArrayBuffer

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.core.WhitespaceTokenizer
import org.apache.lucene.analysis.en.PorterStemFilter
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute

object Normalizer {

    val STOP_WORDS = Set("a", "an", "and", "are", "as", "at", "be", "but", 
                         "by", "for", "if", "in", "into", "is", "it", "no", 
                         "not", "of", "on", "or", "such", "that", "the", 
                         "their", "then", "there", "these", "they", "this", 
                         "to", "was", "will", "with")

    def removeTrailingPunct(word: String): String = {
        if (word.endsWith(",") || word.endsWith(";")) 
            word.substring(0, word.length() - 1)
        else word
    }
    
    def isNumber(s: String): Boolean = {
    	try { 
    	    val x = s.toFloat
    	    true
    	} catch {
    	    case e: Exception => false
    	}
    }
    
    def normalizeCasePunct(name: String): String = {
        name.split("\\s+")
            .map(word => removeTrailingPunct(word))
//            .filter(!isNumber(_))
            .mkString(" ")
            .toLowerCase()
    }
    
    def sortWords(name: String): String = {
        name.split("\\s+")
            .filter(!STOP_WORDS.contains(_))
            .sortWith(_ < _)
            .mkString(" ")
    }
    
    def stemWords(name: String): String = {
        val stemmedWords = ArrayBuffer[String]()
        val analyzer = getAnalyzer()
        val tstream = analyzer.tokenStream("tagname_stm", name)
        val cattr = tstream.addAttribute(classOf[CharTermAttribute])
        try {
            tstream.reset()
            while (tstream.incrementToken()) {
                stemmedWords += cattr.toString()
            }
            tstream.end()
        } finally {
            tstream.close()
        }
        return stemmedWords.mkString(" ")
    }
    
    def getAnalyzer(): Analyzer = {
        new Analyzer() {
            override def createComponents(fieldname: String): TokenStreamComponents = {
                val source = new WhitespaceTokenizer()
                val filter = new PorterStemFilter(source)
                return new TokenStreamComponents(source, filter)
            }
        }
    }
}
