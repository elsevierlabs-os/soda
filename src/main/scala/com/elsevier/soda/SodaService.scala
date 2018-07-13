package com.elsevier.soda

import com.elsevier.soda.messages._
import com.google.gson.Gson
import com.softwaremill.sttp._
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest
import org.apache.solr.common.params.{CommonParams, FacetParams, ModifiableSolrParams}
import org.apache.solr.common.util.{ContentStreamBase, NamedList}
import org.apache.solr.common.{SolrDocumentList, SolrInputDocument}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.springframework.stereotype.Service

import scala.collection.JavaConverters._

@Service
class SodaService {

    val props = SodaUtils.props()
    val solrHost = props("SOLR_HOST")
    val solrPort = props("SOLR_PORT").toInt
    val solrCtx = props("SOLR_CTX")
    val solrIndex = props("SOLR_INDEX")

    val solrIndexHostPorts: Array[String] = if (props.contains("SOLR_INDEX_HOST_PORTS"))
        props("SOLR_WORKER_HOST_PORTS").split(",")
    else Array("%s:%d".format(solrHost, solrPort))

    val solrQueryClient = new HttpSolrClient.Builder("http://%s:%d/%s/%s/"
        .format(solrHost, solrPort, solrCtx, solrIndex))
        .build()
    val solrUpdaterClients = solrIndexHostPorts.map(hostPort =>
        new HttpSolrClient.Builder("http://%s/%s/%s".format(hostPort, solrCtx, solrIndex)).build())


    def checkStatus(): String = {
        implicit val backend = HttpURLConnectionBackend()
        val gson = new Gson()
        try {
            val solrRequest = sttp
                .get(uri"http://$solrHost:$solrPort/solr/admin/info/system")
            val solrResp = solrRequest.send()
            backend.close()

            val solrRespJson = solrResp.body.getOrElse("{}")
            val solrJsonMap = parse(solrRespJson).values.asInstanceOf[Map[String, Any]]

            val response = if (solrJsonMap.isEmpty) {
                IndexResponse("error", "Solr backend is down or unresponsive")
            } else {
                val solrLuceneMap = solrJsonMap("lucene").asInstanceOf[Map[String, Any]]
                val solrVersion = solrLuceneMap("solr-spec-version").asInstanceOf[String]
                IndexResponse("ok", "SoDA accepting requests (Solr version %s)".format(solrVersion))
            }
            gson.toJson(response)
        } catch {
            case e: Exception => {
                val response = IndexResponse("error", "Solr backend is down or unresponsive")
                gson.toJson(response)
            }
        }
    }

    def addEntry(request: String): String = {
        val gson = new Gson()
        val addRequest = gson.fromJson(request, classOf[AddRequest])
        if (addRequest.lexicon == null) {
            gson.toJson(AddResponse("error", "lexicon must be specified", addRequest))
        } else if (addRequest.commit == null) {
            gson.toJson(AddResponse("error", "commit must be either true or false", addRequest))
        } else {
            var numFailed = 0
            if (addRequest.id == null) {
                if (addRequest.commit) {
                    // just commit + no payload
                    solrUpdaterClients.foreach(client => {
                        try {
                            client.commit()
                        } catch {
                            case e: Exception => numFailed += 1
                        }
                    })
                    val numUpdated = solrUpdaterClients.size - numFailed
                    val response = if (numFailed == 0)
                        AddResponse("ok", "(%d/%d) backends updated".format(numUpdated, solrUpdaterClients.size),
                            AddRequest(addRequest.lexicon, addRequest.id, addRequest.names, addRequest.commit))
                    else AddResponse("error", "(%d/%d) backends updated".format(numUpdated, solrUpdaterClients.size),
                        AddRequest(addRequest.lexicon, addRequest.id, addRequest.names, addRequest.commit))
                    gson.toJson(response)
                } else {
                    // don't commit + no payload
                    val response = AddResponse("ok", "Nothing to do",
                        AddRequest(addRequest.lexicon, addRequest.id, addRequest.names, addRequest.commit))
                    gson.toJson(response)
                }
            } else {
                // payload exists, optional commit
                var numFailed = 0
                val idoc = new SolrInputDocument()
                idoc.addField("id", addRequest.id)
                idoc.addField("lexicon", addRequest.lexicon)
                addRequest.names.foreach(name => {
                    idoc.addField("tagname_str", name)
                    // for tagging match (streaming)
                    idoc.addField("tagname_exact", name)
                    idoc.addField("tagname_lower", name)
                    idoc.addField("tagname_stop", name)
                    idoc.addField("tagname_stem1", name)
                    idoc.addField("tagname_stem2", name)
                    idoc.addField("tagname_stem3", name)
                    // for phrase match (non-streaming)
                    idoc.addField("phrname_exact", name)
                    idoc.addField("phrname_lower", name)
                    idoc.addField("phrname_stop", name)
                    idoc.addField("phrname_stem1", name)
                    idoc.addField("phrname_stem2", name)
                    idoc.addField("phrname_stem3", name)
                    val sortedName = SodaUtils.sortWords(name)
                    idoc.addField("phrname_esort", sortedName)
                    idoc.addField("phrname_s3sort", sortedName)
                })
                solrUpdaterClients.foreach(client => {
                    try {
                        client.add(idoc)
                        if (addRequest.commit) client.commit()
                    } catch {
                        case e: Exception => numFailed += 1
                    }
                })
                val numUpdated = solrUpdaterClients.size - numFailed
                val response = if (numFailed == 0)
                    AddResponse("ok", "(%d/%d) backends updated".format(numUpdated, solrUpdaterClients.size),
                        AddRequest(addRequest.lexicon, addRequest.id, addRequest.names, addRequest.commit))
                else AddResponse("error", "(%d/%d) backends updated".format(numUpdated, solrUpdaterClients.size),
                    AddRequest(addRequest.lexicon, addRequest.id, addRequest.names, addRequest.commit))
                gson.toJson(response)
            }
        }
    }

    def deleteEntryOrLexicon(request: String): String = {
        val gson = new Gson()
        val deleteRequest = gson.fromJson(request, classOf[DeleteRequest])
        if (deleteRequest.lexicon == null) {
            gson.toJson(DeleteResponse("error", "lexicon must be specified", deleteRequest))
        } else if (deleteRequest.id == null) {
            gson.toJson(DeleteResponse("error", "id must be specified, * for all", deleteRequest))
        } else {
            var numFailed = 0
            solrUpdaterClients.foreach(client => {
                try {
                    if ("*".equals(deleteRequest.id)) {
                        client.deleteByQuery("lexicon:%s".format(deleteRequest.lexicon))
                    } else {
                        client.deleteById(deleteRequest.id)
                    }
                    client.commit()
                } catch {
                    case e: Exception => numFailed += 1
                }
            })
            val numUpdated = solrUpdaterClients.size - numFailed
            val response = if (numFailed == 0) {
                DeleteResponse("ok", "(%d/%d) backends updated".format(numUpdated, solrUpdaterClients.size),
                    DeleteRequest(deleteRequest.lexicon, deleteRequest.id))
            } else {
                DeleteResponse("error", "(%d/%d) backends updated".format(numUpdated, solrUpdaterClients.size),
                    DeleteRequest(deleteRequest.lexicon, deleteRequest.id))
            }
            gson.toJson(response)
        }
    }

    def annotateText(request: String): String = {
        val gson = new Gson()
        var annotRequest = gson.fromJson(request, classOf[AnnotRequest])
        if (annotRequest.lexicon == null) {
            gson.toJson(AnnotResponse("error", "lexicon must be specified", null))
        } else if (annotRequest.text == null) {
            gson.toJson(AnnotResponse("error", "text must be specified", null))
        } else if (annotRequest.matching == null) {
            gson.toJson(AnnotResponse("error", "matching must be one of exact, lower, stop, stem[1-3]", null))
        } else {
            // send tag request to Solr
            val params = new ModifiableSolrParams()
            params.add("overlaps", "LONGEST_DOMINANT_RIGHT")
            params.add("fq", "lexicon:\"%s\"".format(annotRequest.lexicon))
            params.add("fl", "id,tagname_str")
            params.add("field", "tagname_" + annotRequest.matching)
            val req = new ContentStreamUpdateRequest("")
            val cstream = new ContentStreamBase.StringStream(annotRequest.text)
            cstream.setContentType("text/plain")
            req.addContentStream(cstream)
            req.setMethod(SolrRequest.METHOD.POST)
            req.setPath("/tag")
            req.setParams(params)
            val resp = req.process(solrQueryClient).getResponse()
            // background info needed for confidence calculation
            val id2names = if ("exact".equals(annotRequest.matching)) Map.empty[String, List[String]]
            else asScalaBuffer(resp.get("response").asInstanceOf[SolrDocumentList])
                .map(doc => {
                    val id = doc.getFieldValue("id").asInstanceOf[String]
                    val names = asScalaBuffer(doc.getFieldValues("tagname_str")
                        .asInstanceOf[java.util.ArrayList[String]])
                        .toList
                    (id, names)
                }).toMap
            // parse tag response from Solr
            val annotations = asScalaBuffer(resp.get("tags").asInstanceOf[java.util.List[_]])
                .flatMap(tag => {
                    val tagObj = tag.asInstanceOf[NamedList[_]]
                    val startOffset = tagObj.get("startOffset").asInstanceOf[Int]
                    val endOffset = tagObj.get("endOffset").asInstanceOf[Int]
                    val ids = asScalaBuffer(tagObj.get("ids").asInstanceOf[java.util.List[String]])
                    val coveredText = annotRequest.text.slice(startOffset, endOffset)
                    ids.map(id => {
                        val confidence = SodaUtils.computeConfidence(coveredText, id, id2names,
                            annotRequest.matching)
                        Annotation(id, annotRequest.lexicon, startOffset, endOffset, coveredText, confidence)
                    })
                })
                .toArray
            gson.toJson(AnnotResponse("ok", null, annotations))
        }
    }

    def listLexicons(): String = {
        val gson = new Gson()
        // send request to solr
        val params = new ModifiableSolrParams()
        params.add(CommonParams.Q, "*:*")
        params.add(CommonParams.ROWS, "0")
        params.add(FacetParams.FACET, "true")
        params.add(FacetParams.FACET_FIELD, "lexicon")
        val resp = solrQueryClient.query(params)
        // parse request
        val facetValues = asScalaBuffer(resp.getFacetFields)
            .toList
            .head
            .getValues
        val dictCountPairs = asScalaBuffer(facetValues)
            .map(v => DictCountPair(v.getName, v.getCount))
            .toArray
        gson.toJson(DictResponse("ok", null, dictCountPairs))
    }

    def computeCoverage(request: String): String = {
        val gson = new Gson()
        val coverageRequest = gson.fromJson(request, classOf[CoverageRequest])
        if (coverageRequest.text == null) {
            gson.toJson(CoverageResponse("error", "text must be specified", null))
        } else if (coverageRequest.matching == null) {
            gson.toJson(CoverageResponse("error", "matching must be one of exact, lower, stop, stem[1-3]", null))
        } else {
            // send request to solr
            val params = new ModifiableSolrParams()
            params.add("overlaps", "LONGEST_DOMINANT_RIGHT")
            params.add("fl", "id,lexicon,tagname_str")
            params.add("field", "tagname_" + coverageRequest.matching)
            val req = new ContentStreamUpdateRequest("")
            val cstream = new ContentStreamBase.StringStream(coverageRequest.text)
            cstream.setContentType("text/plain")
            req.addContentStream(cstream)
            req.setMethod(SolrRequest.METHOD.POST)
            req.setPath("/tag")
            req.setParams(params)
            val resp = req.process(solrQueryClient).getResponse()
            // parse response
            val docs = asScalaBuffer(resp.get("response").asInstanceOf[SolrDocumentList])
                .toList
            val coverages = docs.map(doc => (doc.getFieldValue("lexicon")))
                .groupBy(identity)
                .mapValues(_.size)
                .map(x => Coverage(x._1.toString, x._2.toLong))
                .toArray
            gson.toJson(CoverageResponse("ok", null, coverages))
        }
    }

    def lookupLexiconEntry(request: String): String = {
        val gson = new Gson()
        val lookupRequest = gson.fromJson(request, classOf[LookupRequest])
        if (lookupRequest.lexicon == null) {
            gson.toJson(LookupResponse("error", "lexicon must be specified", null))
        } else if (lookupRequest.id == null) {
            gson.toJson(LookupResponse("error", "id must be specified, wildcards ok", null))
        } else {
            val params = new ModifiableSolrParams()
            if ("*".equals(lookupRequest.id)) params.add(CommonParams.Q, "id:*")
            else params.add(CommonParams.Q, "id:%s".format(SodaUtils.escapeLucene(lookupRequest.id)))
            params.add(CommonParams.FQ, "lexicon:%s".format(lookupRequest.lexicon))
            params.add(CommonParams.FL, "id,tagname_str")
            params.add(CommonParams.ROWS, "10")
            val resp = solrQueryClient.query(params)
            val results = asScalaBuffer(resp.getResults()).toList
            val lookupEntries = results.map(doc => {
                val id = doc.getFieldValue("id").asInstanceOf[String]
                val names = asScalaBuffer(doc.getFieldValues("tagname_str")
                    .asInstanceOf[java.util.List[String]])
                    .toArray
                LookupEntry(lookupRequest.lexicon, id, names)
            }).toArray
            gson.toJson(LookupResponse("ok", null, lookupEntries))
        }
    }

    def reverseLookupPhrase(request: String): String = {
        val gson = new Gson()
        val reverseLookupRequest = gson.fromJson(request, classOf[ReverseLookupRequest])
        if (reverseLookupRequest.lexicon == null) {
            gson.toJson(ReverseLookupResponse("error", "lexicon must be specified", null))
        } else if (reverseLookupRequest.phrase == null) {
            gson.toJson(ReverseLookupResponse("error", "phrase must be specified", null))
        } else {
            val params = new ModifiableSolrParams()
            if (reverseLookupRequest.matching.equals("esort") ||
                    reverseLookupRequest.matching.equals("s3sort")) {
                val sortedPhrase = SodaUtils.sortWords(reverseLookupRequest.phrase)
                params.add(CommonParams.Q, """phrname_%s:"%s"""".format(
                    reverseLookupRequest.matching,
                    SodaUtils.escapeLucene(sortedPhrase)))
            } else {
                params.add(CommonParams.Q, """phrname_%s:"%s"""".format(
                    reverseLookupRequest matching,
                    SodaUtils.escapeLucene(reverseLookupRequest.phrase)))
            }
            params.add(CommonParams.FQ, "lexicon:%s".format(reverseLookupRequest.lexicon))
            params.add(CommonParams.FL, "id,tagname_str")
            params.add(CommonParams.ROWS, "10")
            val resp = solrQueryClient.query(params)
            val results = asScalaBuffer(resp.getResults()).toList
            val entries = results.map(doc => {
                val id = doc.getFieldValue("id").asInstanceOf[String]
                val names = asScalaBuffer(doc.getFieldValues("tagname_str")
                    .asInstanceOf[java.util.List[String]])
                    .toArray
                val bestMatchAndConfidence = SodaUtils.computeBestMatchAndConfidence(
                    reverseLookupRequest.phrase, names)
                ReverseLookupEntry(id, reverseLookupRequest.lexicon, bestMatchAndConfidence._1,
                    bestMatchAndConfidence._2)
            })
            gson.toJson(ReverseLookupResponse("ok", null, entries.toArray))
        }

    }
}

