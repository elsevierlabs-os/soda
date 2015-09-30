package com.elsevier.soda

import org.junit.Test
import org.apache.commons.io.FileUtils
import java.io.File
import org.junit.Assert

class AnnotationHelperTest {

    val sodaService = new SodaService()

    @Test
    def testFormatJson(): Unit = {
        val text = FileUtils.readFileToString(
            new File("src/test/resources/sildenafil.txt"))
        val annotations = sodaService.tag(text, "mesh", true)
        Console.println(AnnotationHelper.formatJSON(annotations))
    }
    
    @Test
    def testParseJson(): Unit = {
        val text = FileUtils.readFileToString(
            new File("src/test/resources/sildenafil.txt"))
        val annotations = sodaService.tag(text, "mesh", true)
        val json = AnnotationHelper.formatJSON(annotations)
        val parsedAnnotations = AnnotationHelper.parseJSON(json, text, 0, "mesh")
        Assert.assertEquals(annotations.size, parsedAnnotations.size)
    }
}
