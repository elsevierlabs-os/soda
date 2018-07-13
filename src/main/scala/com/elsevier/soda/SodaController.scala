package com.elsevier.soda

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod}

import scala.collection.JavaConverters._

@Controller
class SodaController @Autowired()(sodaService: SodaService) {

    @RequestMapping(value = Array("/index.json"), method = Array(RequestMethod.GET))
    def index(req: HttpServletRequest, res: HttpServletResponse, model: Model): String = {
        model.addAttribute("response", sodaService.checkStatus())
        "response_view"
    }

    @RequestMapping(value = Array("/add.json"), method = Array(RequestMethod.POST))
    def add(req: HttpServletRequest, res: HttpServletResponse, model: Model): String = {
        val request = asScalaBuffer(IOUtils.readLines(req.getInputStream, "UTF-8"))
            .toList
            .mkString
        model.addAttribute("response", sodaService.addEntry(request))
        "response_view"
    }

    @RequestMapping(value = Array("/delete.json"), method = Array(RequestMethod.POST))
    def delete(req: HttpServletRequest, res: HttpServletResponse, model: Model): String = {
        val request = asScalaBuffer(IOUtils.readLines(req.getInputStream, "UTF-8"))
            .toList
            .mkString
        model.addAttribute("response", sodaService.deleteEntryOrLexicon(request))
        "response_view"
    }

    @RequestMapping(value = Array("/annot.json"), method = Array(RequestMethod.POST))
    def annot(req: HttpServletRequest, res: HttpServletResponse, model: Model): String = {
        val request = asScalaBuffer(IOUtils.readLines(req.getInputStream, "UTF-8"))
            .toList
            .mkString
        model.addAttribute("response", sodaService.annotateText(request))
        "response_view"
    }

    @RequestMapping(value = Array("/dicts.json"), method = Array(RequestMethod.GET))
    def dicts(req: HttpServletRequest, res: HttpServletResponse, model: Model): String = {
        model.addAttribute("response", sodaService.listLexicons())
        "response_view"
    }

    @RequestMapping(value = Array("/coverage.json"), method = Array(RequestMethod.POST))
    def coverage(req: HttpServletRequest, res: HttpServletResponse, model: Model): String = {
        val request = asScalaBuffer(IOUtils.readLines(req.getInputStream, "UTF-8"))
            .toList
            .mkString
        model.addAttribute("response", sodaService.computeCoverage(request))
        "response_view"
    }

    @RequestMapping(value = Array("/lookup.json"), method = Array(RequestMethod.POST))
    def lookup(req: HttpServletRequest, res: HttpServletResponse, model: Model): String = {
        val request = asScalaBuffer(IOUtils.readLines(req.getInputStream, "UTF-8"))
            .toList
            .mkString
        model.addAttribute("response", sodaService.lookupLexiconEntry(request))
        "response_view"
    }

    @RequestMapping(value = Array("/rlookup.json"), method = Array(RequestMethod.POST))
    def rlookup(req: HttpServletRequest, res: HttpServletResponse, model: Model): String = {
        val request = asScalaBuffer(IOUtils.readLines(req.getInputStream, "UTF-8"))
            .toList
            .mkString
        model.addAttribute("response", sodaService.reverseLookupPhrase(request))
        "response_view"
    }
}
