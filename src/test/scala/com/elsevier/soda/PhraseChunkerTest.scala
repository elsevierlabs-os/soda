package com.elsevier.soda

import org.junit.Test
import org.junit.Assert

class PhraseChunkerTest {

    val text = "This text is an attempt to provide you with an overview of the Scala programming language and Scala platform. Since Scala is being developed all the time, this overview page may change over time. Additionally, as I learn more about the Scala language, I may add to this text too. Scala runs on the Java Virtual Machine. Scala is compiled into Java Byte Code which is executed by the Java Virtual Machine (JVM). This means that Scala and Java have a common runtime platform. If you or your organization has standardized on Java, Scala is not going to be a total stranger in your architecture. It's a different language, but the same runtime. Scala can Execute Java Code. Since Scala is compiled into Java Byte Bode, it was a no-brainer for the Scala language designer (Martin Odersky) to enable Scala to be able to call Java code from a Scala program. You can thus use all the classes of the Java SDK's in Scala, and also your own, custom Java classes, or your favourite Java open source projects. Scala Has a Compiler, Interpreter and Runtime. Scala has both a compiler and an interpreter which can execute Scala code."        

    @Test
    def testChunk(): Unit = {
        val chunker = new PhraseChunker()
        val nounPhrases = chunker.phraseChunk(text, "NP")
        Console.println("# of NPs:" + nounPhrases.size)
        Assert.assertEquals(58, nounPhrases.size)
        Assert.assertEquals("This text", nounPhrases.head._1)
        Assert.assertEquals(0, nounPhrases.head._2)
        Assert.assertEquals(9, nounPhrases.head._3)
        nounPhrases.foreach(phrase => Console.println(phrase))
    }
}
