package com.elsevier.soda

import akka.actor.{Actor, ActorSystem, Props}
import com.elsevier.soda.messages.AddRequest
import com.google.gson.Gson
import org.slf4j.LoggerFactory

import scala.io.Source


object SodaBulkLoader extends App {

    if (args.size != 3) {
        val className = getClass.getName.replace("$", "")
        Console.println("Usage: %s lexicon_name /path/to/input_file num_workers".format(className))
        System.exit(0)
    }
    val lexiconName = args(0)
    val inputPath = args(1)
    val numWorkers = args(2).toInt

    if (!validateFormat(inputPath)) {
        Console.println("Input file is malformed, check format")
        System.exit(0)
    }

    val loader = new SodaBulkLoader(lexiconName, inputPath, numWorkers)

    def validateFormat(inputPath: String): Boolean = {
        val src = Source.fromFile(inputPath)
        try {
            val line = src.bufferedReader.readLine()
            // verify that there are two columns tab separated
            val hasValidColumnCount = (line.split("\t").size == 2)
            val hasValidPipeSepCol = (line.split("\t")(1)).split("\\|").size >= 1
            hasValidColumnCount && hasValidPipeSepCol
        } finally {
            src.close()
        }
    }
}

class SodaBulkLoader(lexiconName: String, inputPath: String, numWorkers: Int) {

    val system = ActorSystem("SodaBulkLoader")
    val master = system.actorOf(Props(new Master(lexiconName, inputPath, numWorkers)), name="master")
    master ! StartMsg

}

sealed trait BulkLoadMessage
case class StartMsg() extends BulkLoadMessage
case class IndexMsg(line: String) extends BulkLoadMessage
case class IndexRspMsg(status: Int) extends BulkLoadMessage
case class StopMsg() extends BulkLoadMessage

class Master(lexiconName: String, inputPath: String, numWorkers: Int) extends Actor {

    val logger = LoggerFactory.getLogger(classOf[SodaBulkLoader])

    // create a set of workers
    val workers = List.tabulate(numWorkers)(i => {
        context.actorOf(Props(new Worker(i, lexiconName)), name="worker_%d".format(i))
    })
    workers.foreach(context.watch(_))

    var numReqsSent = 0
    var numRespsRecd = 0
    var numSuccesses = 0
    var numFailures = 0
    var numWorkersTerminated = 0

    def receive = {
        case StartMsg => {
            val src = Source.fromFile(inputPath)
            try {
                 src.getLines
                    .zipWithIndex
                    .foreach(lineIdx => {
                        val line = lineIdx._1
                        val idx = lineIdx._2
                        val workerId = idx % numWorkers
                        workers(workerId) ! IndexMsg(line)
                        numReqsSent += 1
                    })
                workers.foreach(worker => worker ! StopMsg)
            } finally {
                src.close()
            }
        }
        case m: IndexRspMsg => {
            if (m.status == 0) numSuccesses += 1 else numFailures += 1
            numRespsRecd = numSuccesses + numFailures
            if (numRespsRecd % 100 == 0) logger.info("(%d/%d) records processed".format(numRespsRecd, numReqsSent))
        }
        case StopMsg => {
            numWorkersTerminated += 1
            if (numWorkersTerminated >= numWorkers) {
                logger.info("(%d/%d) records processed, COMPLETE".format(numRespsRecd, numReqsSent))
                context.stop(self)
                context.system.terminate
            }
        }
    }
}

class Worker(id: Int, lexiconName: String) extends Actor {

    val sodaService = new SodaService()
    val gson = new Gson()

    var numLinesProcessed = 0

    override def receive = {
        case m: IndexMsg => {
            addEntry(m.line, numLinesProcessed)
            numLinesProcessed += 1
            sender() ! IndexRspMsg(0)
        }
        case StopMsg => {
            commitEntries()
            context.stop(self)
            sender() ! StopMsg
        }
    }

    def addEntry(line: String, numLinesProcessed: Int): Unit = {
        val Array(id, syns) = line.split("\t")
        val names = syns.split("\\|").toArray
        val shouldCommit = (numLinesProcessed % 100 == 0)
        val addRequest = AddRequest(lexiconName, id, names, shouldCommit)
        sodaService.addEntry(gson.toJson(addRequest))
    }

    def commitEntries(): Unit = {
        val commitRequest = AddRequest(lexiconName, null, null, true)
        sodaService.addEntry(gson.toJson(commitRequest))
    }
}