package com.elsevier.soda

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.common.SolrInputDocument

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.fasterxml.jackson.databind.ObjectMapper

object SolrIndexer extends App {

    val sodaProps = SodaUtils.props()
    val SOLR_URL = sodaProps("SOLR_URL")
    val S3_BUCKET_NAME = sodaProps("INDEXDATA_BUCKET_NAME")
    val S3_FOLDER_NAME = sodaProps("INDEXDATA_FOLDER")
    val TAGTYPE = sodaProps("LEX_NAME")

    val accessKey = System.getenv("AWS_ACCESS_KEY")
    val secretKey = System.getenv("AWS_SECRET_KEY")

    if (accessKey == null || secretKey == null) {
        Console.println("AWS_ACCESS_KEY and SECRET_KEY not set, exiting")
    } else {
        val indexer = new SolrIndexer(SOLR_URL, S3_BUCKET_NAME, S3_FOLDER_NAME, 
                                      accessKey, secretKey)
        indexer.deleteLexicon(TAGTYPE, true)
        indexer.index(TAGTYPE)
    }
}

class SolrIndexer(solrUrl: String, s3bucket: String, s3folder: String,
                  accessKey: String, secretKey: String) {

    val solr = new HttpSolrClient(solrUrl)
    val s3 = if (accessKey != null) 
        new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey))
        else null
    
    val jsonMapper = new ObjectMapper()

    def listInputs(): List[String] = {
        if (s3 == null) List()
        else {
            s3.listObjects(new ListObjectsRequest()
                  .withBucketName(s3bucket)
                  .withPrefix(s3folder))
              .getObjectSummaries()
              .toList
              .map(obj => obj.getKey)
              .filter(key => ! key.endsWith("_SUCCESS"))
        }
    }

    def getLines(s3key: String): Iterator[String] = {
        if (s3 == null) List().toIterator
        else {
            val istream = s3.getObject(new GetObjectRequest(s3bucket, s3key))
                            .getObjectContent
            Source.fromInputStream(istream).getLines
        }
    }
    
    def commit(lexName: String): Unit = solr.commit
    
    def deleteLexicon(lexName: String, shouldCommit: Boolean = true): Unit = {
        solr.deleteByQuery("tagtype:%s".format(lexName))
        if (shouldCommit) commit(lexName)
    }
    
    def indexLine(id: String, names: List[String], lexName: String, 
            shouldCommit: Boolean = true): Unit = {
        if (!id.isEmpty && !names.isEmpty) {
            val idoc = new SolrInputDocument()
            // first document
            idoc.addField("id", id)
            idoc.addField("tagsubtype", "x") // exact match
            // we want to remove duplicates so we accumulate
            // instead of directly adding in the names.map() loop
            val ncNames = ArrayBuffer[String]()
            val sortedNames = ArrayBuffer[String]()
            val stemmedNames = ArrayBuffer[String]()
            names.map(name => {
                idoc.addField("tagname_str", name)
                idoc.addField("tagname_stt", name)
                // create and accumulate
                val ncName = Normalizer.normalizeCasePunct(name)
                ncNames += ncName
                val sortedName = Normalizer.sortWords(ncName)
                sortedNames += sortedName
                val stemmedName = Normalizer.stemWords(sortedName)
                stemmedNames += stemmedName
            })
            ncNames.toSet[String]
                .foreach(ncName => idoc.addField("tagname_nrm", ncName))
            sortedNames.toSet[String]
               .foreach(sortedName => idoc.addField("tagname_srt", sortedName))
            stemmedNames.toSet[String]
               .foreach(stemmedName => idoc.addField("tagname_stm", stemmedName))
            idoc.addField("tagtype", lexName)
            solr.add(idoc)
            // additional record for lowercase FST matching, id has
            // trailing _ which needs to be removed by interface before
            // returning
            val idocL = new SolrInputDocument()
            idocL.addField("id", id + "_")
            idocL.addField("tagtype", lexName)
            idocL.addField("tagsubtype", "l")
            names.map(name => {
                idocL.addField("tagname_str", name)
                idocL.addField("tagname_stt", name.toLowerCase())
            })
            solr.add(idocL)
        }
        // commit
        if (shouldCommit) commit(lexName)
    }
    
    def index(lexName: String): Unit = {
        var lineNum = 0
        listInputs.foreach(input => {
            getLines(input).foreach(line => {
                if (lineNum > 0 && lineNum % 100 == 0) {
                    Console.println("%d records inserted...".format(lineNum))
                    solr.commit
                }
                // parse line
                val params = SodaUtils.jsonParse(line)
                indexLine(params("id").asInstanceOf[String],
                    params("names").asInstanceOf[List[String]], 
                    params("lexicon").asInstanceOf[String],
                    false)
                lineNum += 1
            })
        })
        solr.commit
        Console.println("%d records inserted...DONE".format(lineNum))
    }
}
