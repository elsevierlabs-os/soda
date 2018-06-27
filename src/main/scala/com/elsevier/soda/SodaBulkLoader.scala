package com.elsevier.soda

import com.elsevier.soda.messages.AddRequest
import com.google.gson.Gson
import org.slf4j.LoggerFactory

import scala.io.Source

object SodaBulkLoader extends App {

    if (args.size != 2) {
        val className = getClass.getName.replace("$", "")
        Console.println("Usage: %s lexicon_name /path/to/input_file".format(className))
        System.exit(0)
    }
    val lexiconName = args(0)
    val inputPath = args(1)

    val loader = new SodaBulkLoader(lexiconName, inputPath)
    loader.load()
}

class SodaBulkLoader(lexiconName: String, inputPath: String) {

    val logger = LoggerFactory.getLogger(classOf[SodaBulkLoader])

    val sodaService = new SodaService()
    val gson = new Gson()

    def load(): Unit = {
        var numLines = 0
        Source.fromFile(inputPath)
            .getLines
            .foreach(line => {
                val Array(id, syns) = line.split("\t")
                val names = syns.split("\\|").toArray
                val commit = if (numLines % 100 == 0) true else false
                if (commit) logger.info("Loaded %d records for lexicon %s".format(numLines, lexiconName))
                val addRequest = AddRequest(lexiconName, id, names, commit)
                sodaService.addEntry(gson.toJson(addRequest))
                numLines += 1
            })
        val finalCommit = AddRequest(lexiconName, null, null, true)
        sodaService.addEntry(gson.toJson(finalCommit))
        logger.info("Loading %d records for lexicon %s, COMPLETE".format(numLines, lexiconName))
    }
}
