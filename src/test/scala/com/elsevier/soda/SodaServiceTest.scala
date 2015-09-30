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
    
    @Test
    def testPhraseMatches(): Unit = {
        val phrase = "albert einstein"
        val ids = sodaService.getPhraseMatches("wikidata", phrase, "stem")
        Console.println("ids=" + ids)
    }
}
