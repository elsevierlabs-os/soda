package com.elsevier.soda

import java.net.URLDecoder
import java.net.URLEncoder
import com.fasterxml.jackson.databind.ObjectMapper
import scala.collection.JavaConversions._

case class Annotation(namespace: String, id: String, begin: Int, end: Int, 
        props: Map[String,String])

object AnnotationHelper {

    val CoveredText = "covered"
    val Confidence = "conf"
    val Lexicon = "lexicon"
        
    lazy val objectMapper = new ObjectMapper()

    def confToStr(conf: Double) = "%.3f".format(conf)

    
    def formatCAT2(seq: String, annot: Annotation): String = {
        val propString = annot.props.map(kv => 
            List(kv._1, URLEncoder.encode(kv._2, "UTF-8")).mkString("="))
            .mkString("&")
        "%s^%s^%s^%d^%d^%s".format(
            seq, annot.namespace, annot.id, annot.begin, annot.end, propString)

    }
    
    def parseCAT2(line: String): (String, Annotation) = {
        val cols = line.split("\\^")
        val props: Map[String,String] = if (cols.size < 6) Map()
        else cols(5).split("&")
                    .map(kv => kv.split("="))
                    .filter(cols => cols.size == 2)
                    .map(cols => (cols(0), URLDecoder.decode(cols(1), "UTF-8")))
                    .toMap
        (cols(0), Annotation(cols(1), cols(2), cols(3).toInt, 
            cols(4).toInt, props))
    }
    
    def formatJSON(annots: List[Annotation]): String = {
        val alist = new java.util.ArrayList[java.util.HashMap[String,Object]]()
        val annotationAsMaps = annots.foreach(annot => {
            val amap = new java.util.HashMap[String,Object]()
            amap.put("id", annot.id)
            amap.put("begin", new java.lang.Integer(annot.begin))
            amap.put("end", new java.lang.Integer(annot.end))
            amap.put("conf", new java.lang.Double(annot.props(AnnotationHelper.Confidence)))
            alist.add(amap)
        })
        objectMapper.writeValueAsString(alist)
    }
    
    def parseJSON(json: String, text: String, offset: Int, 
            lexName: String): List[Annotation] = {
        objectMapper.readValue(json, classOf[java.util.List[java.util.Map[String,_]]])
          .map(annJson => {
              val annotBegin = annJson.get("begin").asInstanceOf[Int]
              val annotEnd = annJson.get("end").asInstanceOf[Int]
              val annotConf = annJson.get("conf").asInstanceOf[Double]
              val annotId = annJson.get("id").asInstanceOf[String]
              val coveredText = text.substring(annotBegin, annotEnd)
              Annotation("lx", annotId, annotBegin + offset, annotEnd + offset,
                  Map(AnnotationHelper.Confidence -> AnnotationHelper.confToStr(annotConf),
                  AnnotationHelper.CoveredText -> coveredText, 
                  AnnotationHelper.Lexicon -> lexName))
              })
              .toList
        
    }

    def prettyPrintSodaAnnotations(tags: List[Annotation]): Unit = {
        Console.println("%5s %5s %5s %-10s %-25s"
               .format("BEGIN", "END", "CONF", "ENTITY-ID", "MATCHED-TEXT"))
        tags.foreach(tag => prettyPrintSodaAnnotation(tag))
    }
    
    def prettyPrintSodaAnnotation(tag: Annotation): Unit = {
        Console.println("%5d %5d %s %-10s %-25s".format(
            tag.begin, tag.end, tag.props(AnnotationHelper.Confidence), 
            tag.id, tag.props(AnnotationHelper.CoveredText)))
    }
    

}
