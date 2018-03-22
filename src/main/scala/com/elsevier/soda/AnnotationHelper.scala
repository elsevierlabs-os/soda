package com.elsevier.soda

import java.net.URLDecoder
import java.net.URLEncoder
import scala.collection.JavaConversions._

case class Annotation(namespace: String, id: String, begin: Int, end: Int, 
        props: Map[String,String])

object AnnotationHelper {

    val CoveredText = "covered"
    val Confidence = "conf"
    val Lexicon = "lexicon"
    val MatchedText = "matchedText"
        
    def confToStr(conf: Double) = "%.3f".format(conf)

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
