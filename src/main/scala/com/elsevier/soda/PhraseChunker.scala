package com.elsevier.soda

import java.io.InputStream

import scala.Array.canBuildFrom
import scala.collection.mutable.ArrayBuffer

import org.apache.solr.common.util.IOUtils

import opennlp.tools.chunker.ChunkerME
import opennlp.tools.chunker.ChunkerModel
import opennlp.tools.postag.POSModel
import opennlp.tools.postag.POSTaggerME
import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel

class PhraseChunker {

    val modelDir = "opennlp-models"
    
    var sentenceDetector: SentenceDetectorME = null
    var tokenizer: TokenizerME = null
    var postagger: POSTaggerME = null
    var chunker: ChunkerME = null
    
    // set up all models and components
    var smis: InputStream = null
    var tmis: InputStream = null
    var pmis: InputStream = null
    var cmis: InputStream = null
    try {
        smis = getClass.getClassLoader.getResourceAsStream(
            List(modelDir, "en-sent.bin").mkString("/"))
        sentenceDetector = new SentenceDetectorME(new SentenceModel(smis))
        tmis = getClass.getClassLoader.getResourceAsStream(
            List(modelDir, "en-token.bin").mkString("/"))
        tokenizer = new TokenizerME(new TokenizerModel(tmis))
        pmis = getClass.getClassLoader.getResourceAsStream(
            List(modelDir, "en-pos-maxent.bin").mkString("/"))
        postagger = new POSTaggerME(new POSModel(pmis))
        cmis = getClass.getClassLoader.getResourceAsStream(
            List(modelDir, "en-chunker.bin").mkString("/"))
        chunker = new ChunkerME(new ChunkerModel(cmis))
    } finally {
        IOUtils.closeQuietly(smis)
        IOUtils.closeQuietly(tmis)
        IOUtils.closeQuietly(pmis)
        IOUtils.closeQuietly(cmis)
    }
        
    def phraseChunk(text: String, 
            phraseType: String): List[(String,Int,Int)] = {
        val phrases = ArrayBuffer[(String,Int,Int)]()
        val sentenceSpans = sentenceDetector.sentPosDetect(text)
        sentenceSpans.foreach(sentenceSpan => {
            val sentence = sentenceSpan.getCoveredText(text).toString
            val sentenceStart = sentenceSpan.getStart()
            val tokenSpans = tokenizer.tokenizePos(sentence)
            val tokens = tokenSpans.map(tokenSpan => 
                tokenSpan.getCoveredText(sentence).toString())
            val postags = postagger.tag(tokens)
            val chunks = chunker.chunkAsSpans(tokens, postags)
            chunks.foreach(chunk => {
                val start = sentenceStart + tokenSpans(chunk.getStart()).getStart()
                val end = sentenceStart + tokenSpans(chunk.getEnd() - 1).getEnd()
                val chunkText = text.substring(start, end)
                if (phraseType == null || phraseType.equals(chunk.getType()))
                    phrases += ((chunkText, start, end))
            })
        })
        phrases.toList
    }
}
