package com.elsevier.soda

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.mutable.ArrayBuffer
import org.apache.commons.lang3.StringUtils
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.params.CommonParams
import org.apache.solr.common.params.FacetParams
import org.apache.solr.common.params.ModifiableSolrParams
import org.apache.solr.common.util.ContentStreamBase
import org.apache.solr.common.util.NamedList
import org.springframework.stereotype.Service
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException
import java.util.Collections
import org.apache.solr.common.util.ContentStream
import org.apache.solr.common.SolrInputDocument
import java.util.regex.Pattern
import com.aliasi.chunk.RegExChunker

case class DictInfo(dictName: String, numEntries: Long)

@Service
class SodaService {

    val props = SodaUtils.props()
    val solrQueryUrl = props("SOLR_QUERY_URL")
    val solrUpdateUrls = props("SOLR_INDEX_URL").split(",").toList

    val querySolr = new HttpSolrClient(solrQueryUrl)
    val updateSolrs = solrUpdateUrls.map(url => new HttpSolrClient(url))
    
    val phraseChunker = new PhraseChunker()
    val stopwords = SodaUtils.stopwords()
    val sodaClient = new SodaClient()

    def annotate(text: String, lexiconName: String, 
            matchFlag: String): List[Annotation] = {
        val lexName = if (lexiconName.endsWith("-full")) 
                          lexiconName.substring(0, lexiconName.length() - 5)
                      else lexiconName
        val annotations = matchFlag match {
            case "exact" => tag(text, lexName, false)
            case "lower" => tag(text, lexName, true)
            case "punct" => chunkAndTag(text, lexName, "tagname_nrm")
            case "sort" => chunkAndTag(text, lexName, "tagname_srt")
            case "stem" => chunkAndTag(text, lexName, "tagname_stm")
            case _ => List()
        }
        if (lexiconName.endsWith("-full")) annotations
        else {
            annotations.filter(annotation => {
                val coveredText = annotation.props(AnnotationHelper.CoveredText)
                val isAbbrev = SodaUtils.isAbbreviation(coveredText)
                val isStopword = SodaUtils.isStopword(coveredText, stopwords)
                val isTooShort = SodaUtils.isTooShort(coveredText)
                isAbbrev || !(isStopword || isTooShort)
            })
        }
    }

    def tag(text: String, lexName: String, 
            lowerCaseInput: Boolean): List[Annotation] = {
        val params = new ModifiableSolrParams()
        params.add("overlaps", "LONGEST_DOMINANT_RIGHT")
        params.add("fq", buildFq(lexName, lowerCaseInput))
        params.add("fl", "id,tagtype,tagname_str")
        val req = new ContentStreamUpdateRequest("")
        val cstream = new ContentStreamBase.StringStream(
            if (lowerCaseInput) text.toLowerCase() else text)
        cstream.setContentType("text/plain")
        req.addContentStream(cstream)
        req.setMethod(SolrRequest.METHOD.POST)
        req.setPath("/tag")
        req.setParams(params)
        val resp = req.process(querySolr).getResponse()
        // extract the tags
        val tags = resp.get("tags").asInstanceOf[java.util.ArrayList[_]]
            .flatMap(tagInfo => {
                val tagList = tagInfo.asInstanceOf[NamedList[_]]
                val startOffset = tagList.get("startOffset").asInstanceOf[Int]
                val endOffset = tagList.get("endOffset").asInstanceOf[Int]
                val ids = tagList.get("ids").asInstanceOf[java.util.ArrayList[String]]
                ids.map(id => (id, startOffset, endOffset))
            }).toList
        // attach the confidences. If we are doing exact match, then we
        // can be lazy and attach confidence of 1 because these are exact
        // matches, otherwise we need to extract the tagname_str and calculate
        if (lowerCaseInput) {
            val idNameMap = resp.get("response").asInstanceOf[SolrDocumentList]
                .iterator()
                .map(doc => {
                    val id = doc.getFieldValue("id").asInstanceOf[String]
                    val tagType = doc.getFieldValues("tagtype")
                        .map(_.asInstanceOf[String])
                        .filter(_.equals(lexName))
                        .head
                    val names = doc.getFieldValues("tagname_str")
                        .map(_.asInstanceOf[String])
                        .toList
                    (id, (tagType, names))               
                }).toMap
            // remove trailing _ from id for lowercase records
            tags.map(tag => {
                val coveredText = text.substring(tag._2, tag._3)
                val conf = bestScore(coveredText, idNameMap(tag._1)._2) 
                Annotation("lx", tag._1.replace("_", ""), tag._2, tag._3,
                    Map(AnnotationHelper.CoveredText -> coveredText,
                        AnnotationHelper.Confidence -> AnnotationHelper.confToStr(conf),
                        AnnotationHelper.Lexicon -> idNameMap(tag._1)._1))
            })
        } else {
            val idTagtypeMap = resp.get("response")
                .asInstanceOf[SolrDocumentList]
                .iterator()
                .map(doc => {
                    val id = doc.getFieldValue("id").asInstanceOf[String]
                    val tagtype = doc.getFieldValues("tagtype")
                        .map(_.asInstanceOf[String])
                        .filter(_.equals(lexName))
                        .head
                    (id, tagtype)
                }).toMap
            tags.map(tag => {
                val coveredText = text.substring(tag._2, tag._3)
                Annotation("lx", tag._1, tag._2, tag._3, 
                    Map(AnnotationHelper.CoveredText -> coveredText, 
                        AnnotationHelper.Confidence -> "1.0",
                        AnnotationHelper.Lexicon -> idTagtypeMap(tag._1)))
                })
        }
    }
    
    def chunkAndTag(text: String, lexName: String, 
            matchOnField: String): List[Annotation] = {
        val phrases = phraseChunker.phraseChunk(text, "NP")
        val transformedPhrases = phrases.map(phrase => {
            val suffix = matchOnField.substring(matchOnField.lastIndexOf("_"))
            val transformedPhrase = suffix match {
                case "_nrm" => Normalizer.normalizeCasePunct(phrase._1)
                case "_srt" => Normalizer.sortWords(
                    Normalizer.normalizeCasePunct(phrase._1))
                case "_stm" => Normalizer.stemWords(
                    Normalizer.sortWords(
                    Normalizer.normalizeCasePunct(phrase._1)))
                  case _ => null
            }
            (transformedPhrase, phrase._2, phrase._3)
        })
        // run each of these phrases against
        val tags = ArrayBuffer[Annotation]()
        transformedPhrases.foreach(phrase => {
            val params = new ModifiableSolrParams()
            params.add(CommonParams.Q, matchOnField + ":\"" + phrase._1 + "\"")
            params.add(CommonParams.ROWS, "1")
            params.add(CommonParams.FQ, buildFq(lexName, false))
            params.add(CommonParams.FL, "id,tagname_str")
            val resp = querySolr.query(params)
            val results = resp.getResults()
            if (results.getNumFound() > 0) {
                val sdoc = results.get(0)
                val id = sdoc.getFieldValue("id").asInstanceOf[String]
                val names = sdoc.getFieldValues("tagname_str")
                    .map(_.asInstanceOf[String])
                    .toList
                val coveredText = text.substring(phrase._2, phrase._3) 
                val score = bestScore(phrase._1, names)
                tags += (Annotation("lx", id, phrase._2, phrase._3, 
                    Map(AnnotationHelper.CoveredText -> coveredText,
                        AnnotationHelper.Confidence -> AnnotationHelper.confToStr(score),
                        AnnotationHelper.Lexicon -> lexName)))
            }
        })
        tags.filter(annot => {
            val coveredText = annot.props(AnnotationHelper.CoveredText)
                .replaceAll("\\p{Punct}", "")
                .toLowerCase()
            coveredText.trim().length() > 3 && !stopwords.contains(coveredText)    
        }).toList
    }
    
    def buildFq(tagtype: String, lowerCaseInput: Boolean): String = {
        val tagSubtypeQuery = Array("tagsubtype", if (lowerCaseInput) "l" else "x")
                                  .mkString(":")
        if (tagtype == null) {
            tagSubtypeQuery
        } else {
            val tagtypeQuery = Array("tagtype", tagtype.toLowerCase())
                                   .mkString(":")
            return Array(tagtypeQuery, tagSubtypeQuery).mkString(" AND ")
        }
    }
    
    def bestScore(matchedSpan: String, names: List[String]): Double = {
        val score = names.map(name =>
                StringUtils.getLevenshteinDistance(matchedSpan, name))
            .sorted
            .head
        if (matchedSpan.length() == 0) 0.0D
        else if (score > matchedSpan.length()) 0.0D
        else (1.0D - (1.0D * score / matchedSpan.length()))                               
    }
    
    def getDictInfo(): List[DictInfo] = {
        val params = new ModifiableSolrParams()
        params.add(CommonParams.Q, "*:*")
        params.add(CommonParams.FQ, "tagsubtype:x")
        params.add(CommonParams.ROWS, "0")
        params.add(FacetParams.FACET, "true")
        params.add(FacetParams.FACET_FIELD, "tagtype")
        val resp = querySolr.query(params)
        resp.getFacetFields().head
            .getValues()
            .map(v => DictInfo(v.getName(), v.getCount()))
            .toList
    }
    
    def getCoverageInfo(text: String): List[DictInfo] = {
        val params = new ModifiableSolrParams()
        params.add("overlaps", "LONGEST_DOMINANT_RIGHT")
        params.add("fl", "id,tagtype,tagname_str")
        val req = new ContentStreamUpdateRequest("")
        val cstream = new ContentStreamBase.StringStream(text)
        cstream.setContentType("text/plain")
        req.addContentStream(cstream)
        req.setMethod(SolrRequest.METHOD.POST)
        req.setPath("/tag")
        req.setParams(params)
        val resp = req.process(querySolr).getResponse()
        resp.get("response")
            .asInstanceOf[SolrDocumentList]
            .iterator()
            .flatMap(doc => {
                doc.getFieldValues("tagtype")
                   .map(_.asInstanceOf[String])
                   .map(tt => (tt, 1))
            })
            .toList
            .groupBy(kv => kv._1)
            .map(kv => DictInfo(kv._1, kv._2.size))
            .toList
    }
    
    def getNames(lexName: String, id: String): List[String] = {
        val params = new ModifiableSolrParams()
        params.add("q", "id:%s".format(SodaUtils.escapeLucene(id)))
        params.add("fq", "tagtype:%s".format(lexName))
        params.add("fl", "tagname_str")
        params.add("rows", "1")
        val resp = querySolr.query(params)
        val results = resp.getResults()
        if (results.getNumFound() == 0) List()
        else {
            results.get(0)
                   .getFieldValues("tagname_str")
                   .map(v => v.asInstanceOf[String])
                   .toList
        }
    }
    
    def getPhraseMatches(lexName: String, phrase: String, matching: String): 
            List[Annotation] = {
        val fieldName = matching match {
            case "exact" => "tagname_str"
            case "lower" => "tagname_str"
            case "punct" => "tagname_nrm"
            case "sort" => "tagname_srt"
            case "stem" => "tagname_stm"
        }
        val suffix = fieldName.substring(fieldName.lastIndexOf("_"))
        val fieldValue = suffix match {
            case "_nrm" => Normalizer.normalizeCasePunct(phrase)
            case "_srt" => Normalizer.sortWords(
                    Normalizer.normalizeCasePunct(phrase))
            case "_stm" => Normalizer.stemWords(
                Normalizer.sortWords(
                Normalizer.normalizeCasePunct(phrase)))
            case _ => phrase
        }
        val params = new ModifiableSolrParams()
        params.add("q", "%s:\"%s\"".format(fieldName, 
            SodaUtils.escapeLucene(fieldValue)))
        params.add("fq", "tagtype:%s".format(lexName))
        params.add("fl", "id,score")
        params.add("rows", "5")
        val resp = querySolr.query(params)
        val results = resp.getResults()
        if (results.getNumFound() == 0) List()
        else {
            val ids = results.iterator()
                .map(doc => {
                    val id = doc.get("id").asInstanceOf[String]
                    if (!id.endsWith("_")) id
                         else id.substring(id.length - 1)
                         
                }).toList.distinct
            ids.map(id => {
                val aprops = Map(
                    AnnotationHelper.CoveredText -> phrase,
                    AnnotationHelper.Confidence -> "0.0",
                    AnnotationHelper.Lexicon -> lexName 
                )
                Annotation("lx", id, 0, 0, aprops)
            })
        }
    }
    
    def regex(text: String, patterns: Map[String, String]): List[Annotation] = {
        patterns.map(p => {
                val regex = Pattern.compile(p._2)
                new RegExChunker(regex, p._1, 1.0D)
            })
            .flatMap(chunker => {
                val chunking = chunker.chunk(text)
                chunking.chunkSet.map(chunk => {
                    val start = chunk.start
                    val end = chunk.end
                    val matchedRegex = chunk.`type`
                    val covered = text.substring(start, end)
                    Annotation("lx", "#", start, end, 
                        Map(AnnotationHelper.CoveredText -> covered, 
                            AnnotationHelper.Confidence -> "1.0",
                            AnnotationHelper.Lexicon -> matchedRegex))
                })
            }).toList
    }
    
    // update methods
    
    def delete(lexName: String, shouldCommit: Boolean = true): Unit = {
        updateSolrs.foreach(updateSolr => {
            updateSolr.deleteByQuery("tagtype:%s".format(lexName))
            if (shouldCommit) updateSolr.commit()
        })
    }
    
    def add(id: String, names: List[String], lexName: String, 
            shouldCommit: Boolean): Unit = {
        updateSolrs.foreach(updateSolr => {
            if (!id.isEmpty && !names.isEmpty) {
                val idoc = new SolrInputDocument()
                // first document
                idoc.addField("id", id)
                idoc.addField("tagtype", lexName)
                idoc.addField("tagsubtype", "x") // exact match
                val ncNames = ArrayBuffer[String]()
                val sortedNames = ArrayBuffer[String]()
                val stemmedNames = ArrayBuffer[String]()
                names.map(name => {
                    idoc.addField("tagname_str", name)
                    idoc.addField("tagname_stt", name)
                    val ncName = Normalizer.normalizeCasePunct(name)
                    if (!ncName.isEmpty) ncNames += ncName
                    val sortedName = Normalizer.sortWords(ncName)
                    if (!sortedName.isEmpty) sortedNames += sortedName
                    val stemmedName = Normalizer.stemWords(sortedName)
                    if (!stemmedName.isEmpty) stemmedNames += stemmedName
                    ncNames.toSet[String].foreach(ncName => 
                        idoc.addField("tagname_nrm", ncName))
                    sortedNames.toSet[String].foreach(sortedName => 
                        idoc.addField("tagname_srt", sortedName))
                    stemmedNames.toSet[String].foreach(stemmedName => 
                        idoc.addField("tagname_stm", stemmedName))
                })
                updateSolr.add(idoc)
                // second document
                // additional record for lowercase FST matching, id has
                // trailing _ that is removed by interface
                val idoc2 = new SolrInputDocument()
                idoc2.addField("id", id + "_")
                idoc2.addField("tagtype", lexName)
                idoc2.addField("tagsubtype", "l") // lowercase match
                names.filter(name => !name.isEmpty)
                    .map(name => {
                        idoc2.addField("tagname_str", name)
                        idoc2.addField("tagname_stt", name.toLowerCase())
                    })
                updateSolr.add(idoc2)
            }
            if (shouldCommit) updateSolr.commit()
        })
    }
    
    def annotJson(annots: List[Annotation]): String = {
        "[" + annots.map(annot => sodaClient.jsonBuild(Map(
            "id" -> annot.id,
            "begin" -> annot.begin,
            "end" -> annot.end,
            "coveredText" -> annot.props(AnnotationHelper.CoveredText),
            "confidence" -> annot.props(AnnotationHelper.Confidence),
            "lexicon" -> annot.props(AnnotationHelper.Lexicon)
        ))).mkString(",") + "]"
    }
}
