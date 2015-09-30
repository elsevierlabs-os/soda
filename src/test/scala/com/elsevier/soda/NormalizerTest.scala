package com.elsevier.soda

import org.junit.Assert
import org.junit.Test

class NormalizerTest {

    @Test
    def testPunctCaseNormalize(): Unit = {
        val inp = "This, is the house that Jack, Esq,, built."
        val act = "this  is the house that jack  esq   built "
        val prd = Normalizer.normalizeCasePunct(inp)
        Console.println("input: [%s]\nnormalized: [%s]".format(inp, prd))
        Assert.assertEquals(act, prd)
    }
    
    @Test
    def testSortWords(): Unit = {
        val inp = "this  is the house that jack  esq   built "
        val act = "built esq house is jack that the this"
        val prd = Normalizer.sortWords(inp)
        Console.println("input: [%s]\nsorted: [%s]".format(inp, prd))
        Assert.assertEquals(act, prd)
    }
    
    @Test
    def testStemWords(): Unit = {
        val inp = "built esq house is jack that the this"
        val act = "built esq hous is jack that the thi"
        val prd = Normalizer.stemWords(inp)
        Console.println("input: [%s]\nstemmed: [%s]".format(inp, prd))
        Assert.assertEquals(act, prd)
    }
}
