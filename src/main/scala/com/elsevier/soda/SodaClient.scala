package com.elsevier.soda

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import scala.collection.JavaConversions._
import org.apache.commons.io.IOUtils
import scala.io.Source

import org.json4s._
//import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.native.Json
//import org.json4s.DefaultFormats

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
    
    // http://stackoverflow.com/questions/27948128/how-to-convert-scala-map-into-json-string
    //http://json4s.org/
    
    def jsonBuild(params: Map[String, Any]): String = {
        Json(DefaultFormats).write(params)
    }
    
    def jsonParse(json: String): Map[String, Any] = {
        implicit val formats = DefaultFormats
        parse(json).extract[Map[String, Any]]
    }
    
    def jsonParseList(json: String): List[Map[String, Any]] = {
        implicit val formats = DefaultFormats
        parse(json).extract[List[Map[String, Any]]]
    }
}
