package com.elsevier.soda

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import scala.collection.JavaConversions.asScalaBuffer
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import com.fasterxml.jackson.databind.ObjectMapper

@Controller
class SodaController @Autowired() (sodaService: SodaService) {
    
    val SOLR_URL = "http://localhost:8983/solr/texttagger"
        
    val jsonMapper = new ObjectMapper()
    val solrIndexer = new SolrIndexer(SOLR_URL, null, null, null, null)
    
    @RequestMapping(value=Array("/index.json"), method=Array(RequestMethod.GET))
    def index(req: HttpServletRequest, res: HttpServletResponse, 
            model: Model): String = {
        model.addAttribute("response", SodaUtils.OK)
        "annotate"
    }
    
    @RequestMapping(value=Array("/dicts.json"), method=Array(RequestMethod.GET))
    def dicts(req: HttpServletRequest, res: HttpServletResponse, 
            model: Model): String = {
        val dictInfos = "[" + sodaService.getDictInfo()
            .map(dictInfo => SodaUtils.jsonBuild(Map(
                "lexicon" -> dictInfo.dictName, 
                "numEntries" -> dictInfo.numEntries)))
            .mkString(", ") + "]"
        model.addAttribute("response", dictInfos)
        "annotate"
    }
    
    @RequestMapping(value=Array("/annot.json"), method=Array(RequestMethod.POST))
    def annot(req: HttpServletRequest, res: HttpServletResponse, 
            model: Model): String = {
        val params = SodaUtils.jsonParse(
            IOUtils.readLines(req.getInputStream()).mkString)
        val lexicon = params.getOrElse("lexicon", "").asInstanceOf[String]
        val text = params.getOrElse("text", "").asInstanceOf[String]
        val matchFlag = params.getOrElse("matching", "exact").asInstanceOf[String]
        if (lexicon.isEmpty || text.isEmpty) {
            model.addAttribute("response", 
                SodaUtils.error("Both Lexicon and Text must be specified!"))
        } else {
            val respAnns = "[" + 
                sodaService.annotate(text, lexicon, matchFlag)
                    .map(annotation => SodaUtils.jsonBuild(Map(
                        "id" -> annotation.id,
                        "begin" -> annotation.begin,
                        "end" -> annotation.end,
                        "coveredText" -> annotation.props(AnnotationHelper.CoveredText),
                        "confidence" -> annotation.props(AnnotationHelper.Confidence),
                        "lexicon" -> annotation.props(AnnotationHelper.Lexicon)
                ))).mkString(", ") + "]"
            model.addAttribute("response", respAnns)
        }
        "annotate"
    }
    
    @RequestMapping(value=Array("/lookup.json"), method=Array(RequestMethod.POST))
    def lookup(req: HttpServletRequest, res: HttpServletResponse, 
            model: Model): String = {
        val params = SodaUtils.jsonParse(
            IOUtils.readLines(req.getInputStream()).mkString)
        val lexicon = params.getOrElse("lexicon", "").asInstanceOf[String]
        val id = params.getOrElse("id", "").asInstanceOf[String]
        if (lexicon.isEmpty) {
            model.addAttribute("response", 
                SodaUtils.error("Lexicon must be specified!"))
        } else if (id.isEmpty) {
            model.addAttribute("response", 
                SodaUtils.error("Id must be specified!"))
        } else {
            val names = sodaService.getNames(lexicon, id)
            val resp = Map(
                "status" -> "ok",
                "id" -> id,
                "lexicon" -> lexicon,
                "names" -> names)
            model.addAttribute("response", SodaUtils.jsonBuild(resp))
        }
        "annotate"
    }
    
    @RequestMapping(value=Array("/matchphrase.json"), method=Array(RequestMethod.POST))
    def matchPhrase(req: HttpServletRequest, res: HttpServletResponse,
            model: Model): String = {
        val s = IOUtils.readLines(req.getInputStream()).mkString
        val params = SodaUtils.jsonParse(s)        
        val lexicon = params.getOrElse("lexicon", "").asInstanceOf[String]
        val text = params.getOrElse("text", "").asInstanceOf[String]
        val matching = params.getOrElse("matching", "exact").asInstanceOf[String]
        if (lexicon.isEmpty) {
            model.addAttribute("response", 
            	SodaUtils.error("Lexicon must be specified"))
        } else if (text.isEmpty) {
            model.addAttribute("response", 
            	SodaUtils.error("Text must be specified"))
        } else {
            val ids = sodaService.getPhraseMatches(lexicon, text, matching)
            val resp = Map(
            	"status" -> "ok",
            	"text" -> text,
            	"lexicon" -> lexicon,
            	"matching" -> matching,
            	"ids" -> ids)
            model.addAttribute("response", SodaUtils.jsonBuild(resp))
        }
        "annotate"
    }

    @RequestMapping(value=Array("/delete.json"), method=Array(RequestMethod.POST))
    def delete(req: HttpServletRequest, res: HttpServletResponse,
            model: Model): String = {
        val params = SodaUtils.jsonParse(
            IOUtils.readLines(req.getInputStream()).mkString)
        val lexicon = params.getOrElse("lexicon", "").asInstanceOf[String]
        if (lexicon.isEmpty) {
            model.addAttribute("response", 
                SodaUtils.error("Lexicon must be specified!"))
        } else {
            solrIndexer.deleteLexicon(lexicon)
            model.addAttribute("response", SodaUtils.OK)
        }
        "annotate"
    }
    
    @RequestMapping(value=Array("/add.json"), method=Array(RequestMethod.POST))
    def add(req: HttpServletRequest, res: HttpServletResponse, 
            model: Model): String = {
        val s = IOUtils.readLines(req.getInputStream()).mkString
        val params = SodaUtils.jsonParse(s)
        val id = params.getOrElse("id", "").asInstanceOf[String]
        val lexicon = params.getOrElse("lexicon", "").asInstanceOf[String]
        val names = params.getOrElse("names", List.empty).asInstanceOf[List[String]]
        val shouldCommit = params.getOrElse("commit", true).asInstanceOf[Boolean]
        if (lexicon.isEmpty) {
            model.addAttribute("response", 
                SodaUtils.error("Lexicon must be specified!"))
        } else if ((id.isEmpty && !names.isEmpty) || 
                   (!id.isEmpty && names.isEmpty)) {
            model.addAttribute("response", 
               SodaUtils.error("Both id and names should be specified!"))
        } else {
            solrIndexer.indexLine(id, names, lexicon, shouldCommit)
            model.addAttribute("response", SodaUtils.OK)
        }
        "annotate"
    }
    
    @RequestMapping(value=Array("/coverage.json"), method=Array(RequestMethod.POST))
    def coverage(req: HttpServletRequest, res: HttpServletResponse, 
            model: Model): String = {
        val params = SodaUtils.jsonParse(
            IOUtils.readLines(req.getInputStream()).mkString)
        val text = params.getOrElse("text", "").asInstanceOf[String]
        if (text.isEmpty) {
            model.addAttribute("response", 
                SodaUtils.error("Text must be specified!"))
        } else {
            val coverageInfos = "[" + sodaService.getCoverageInfo(text)
                .map(ci => SodaUtils.jsonBuild(Map("lexicon" -> ci.dictName, 
                    "numEntries" -> ci.numEntries)))
                .mkString(", ") + "]"
            model.addAttribute("response", coverageInfos)
        }
        "annotate"
    }
}
