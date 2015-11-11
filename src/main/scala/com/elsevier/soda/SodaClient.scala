package com.elsevier.soda

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import scala.collection.JavaConversions._
import org.apache.commons.io.IOUtils
import scala.io.Source
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.native.Json
import scala.util.parsing.json.JSON

class SodaClient extends java.io.Serializable {
    
    def get(url: String, params: Map[String, String] = Map.empty): String = {
        val parameters = if (params.isEmpty) ""
        else "?" + params.map(kv => 
            List(kv._1, URLEncoder.encode(kv._2, "UTF-8")).mkString("="))
            .mkString("&")
        Source.fromURL(url + parameters).getLines.mkString
    } 
    
    def post(url: String, json: String): String = {
        var conn: HttpURLConnection = null
        try {
            // set up connection
            val soda = new URL(url)
            conn = soda.openConnection().asInstanceOf[HttpURLConnection]
            conn.setRequestMethod("POST")
            conn.setRequestProperty("Content-Type", "multipart/form-data")
            conn.setRequestProperty("Content-Length", json.length().toString)
            conn.setUseCaches(false)
            conn.setDoInput(true)
            conn.setDoOutput(true)
            // write input into connection
            IOUtils.write(json, conn.getOutputStream())
            // return response
            IOUtils.readLines(conn.getInputStream()).mkString
        } catch {
            case e: Exception => SodaUtils.error(e.getMessage)
        } finally {
            if (conn != null) conn.disconnect()
        }
    }
    
    def jsonBuild(params: Map[String, Any]): String = {
        Json(DefaultFormats).write(params)
    }
    
    // json4s is broken for version 3.10 which Spark requires, the
    // bug was fixed in 3.11 but Spark 1.5 still uses json4s 3.10.
    
//    def jsonParse(json: String): Map[String, Any] = {
//        implicit val formats = DefaultFormats
//        parse(json).extract[Map[String, Any]]
//    }
//    
//    def jsonParseList(json: String): List[Map[String, Any]] = {
//        implicit val formats = DefaultFormats
//        Json(DefaultFormats).parse(json).extract[List[Map[String, Any]]]
//    }

    def jsonParse(json: String): Map[String, Any] = {
        JSON.parseFull(json) match {
            case Some(e) => e.asInstanceOf[Map[String, Any]]
            case None => Map()
        }
    }
    
    def jsonParseList(json: String): List[Map[String, Any]] = {
        JSON.parseFull(json) match {
            case Some(e) => e.asInstanceOf[List[Map[String, Any]]]
            case None => List()
        }
    }
}
