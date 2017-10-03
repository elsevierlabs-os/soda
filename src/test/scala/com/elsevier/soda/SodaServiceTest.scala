package com.elsevier.soda

import org.junit.Test
import org.apache.commons.io.FileUtils
import java.io.File
import scala.io.Source

class SodaServiceTest {

    val sodaService = new SodaService()
    
//    @Test
    def testTagging(): Unit = {
        val text = FileUtils.readFileToString(
            new File("src/test/resources/sildenafil.txt"))
        val tags = sodaService.tag(text, "mesh", true)
        AnnotationHelper.prettyPrintSodaAnnotations(tags)
    }

    @Test
    def testChunkTaggingSort(): Unit = {
        val text = FileUtils.readFileToString(
            new File("src/test/resources/sildenafil.txt"))
        val tags = sodaService.chunkAndTag(text, "mesh", "tagname_srt")
        AnnotationHelper.prettyPrintSodaAnnotations(tags)
    }
    
//    @Test
    def testChunkTaggingStem(): Unit = {
        val text = FileUtils.readFileToString(
            new File("src/test/resources/sildenafil.txt"))
        val tags = sodaService.chunkAndTag(text, "mesh", "tagname_stm")
        AnnotationHelper.prettyPrintSodaAnnotations(tags)
    }
    
//    @Test
    def testBatchTagging(): Unit = {
        Source.fromFile("src/test/resources/example.csv")
              .getLines
              .map(line => line.split("\t")(2))
              .map(text => (sodaService.tag(text, "mesh", true), text))
              .foreach(results => 
                   AnnotationHelper.prettyPrintSodaAnnotations(results._1))
    }

//    @Test
    def testTaggingQuality(): Unit = {
        val text = "Fracture energy versus volume fraction for the three particle sizes of silica nanoparticles in the epoxy polymers.Fig. 2"
        val annotations = sodaService.tag(text, "wiki", false)
        annotations.foreach(ann => AnnotationHelper.prettyPrintSodaAnnotation(ann))
    }
    
//    @Test
    def testPhraseMatches(): Unit = {
        val phrase = "albert einstein"
        val ids = sodaService.getPhraseMatches("wikidata", phrase, "stem")
        Console.println("ids=" + ids)
    }

//    @Test
    def testPhraseMatchesWithSort(): Unit = {
        val phrase = "marie curie"
        val ids = sodaService.getPhraseMatches("wikidata", phrase, "sort")
        Console.println("ids=" + ids)
    }

//    @Test
    def testRegexTagging1(): Unit = {
       val patterns = Map(
           "ZIPCODE" -> "\\d{5}(-\\d{4})?",
           "GENE" -> "[A-Z][A-Z0-9_]+"
       )
       val texts = List(
           "Mr Smith went to Washington.",
           "Beverley Hills 90210 is a great show.",
           "H1N1 can lead to death."
       )
       texts.foreach(text => {
           Console.println(text)
           val annots = sodaService.regex(text, patterns)
           annots.foreach(a => Console.println("(%d, %d) [%s] %s"
               .format(a.begin, a.end, a.namespace, a.props("covered"))))
       })
    }
    
//    @Test
    def testRegexTagging2(): Unit = {
        val testDataDir = "/Users/palsujit/Elsevier/OA-STM-Corpus/SimpleText/SimpleText_auto"
        val doiPattern = Map("DOI" -> "doi:10\\.\\d{4,}\\/[a-z0-9\\.]+" )
        new File(testDataDir).listFiles
            .map(file => {
                Console.println("File: " + file.getName)
                val text = Source.fromFile(file).getLines.mkString("\n")
                val annots = sodaService.regex(text, doiPattern)
                annots.foreach(a => Console.println("(%d, %d) [%s] %s"
                    .format(a.begin, a.end, a.namespace, a.props("covered"))))
            })
    }
}
