package com.elsevier.soda

import com.elsevier.soda.messages._
import com.google.gson.Gson
import com.softwaremill.sttp._

final case class SodaClientException(private val message: String = "",
                                     private val cause: Throwable = None.orNull)
    extends Exception(message, cause)


class SodaClient(sodaUrl: String) extends java.io.Serializable {

    val sodaUrlPrefix = sodaUrl

    implicit val backend = HttpURLConnectionBackend()
    val gson = new Gson()


    def index(): IndexResponse = {
        val request = sttp.get(uri"$sodaUrlPrefix/index.json")
        val response = request.send()
        val resp = response.body match {
            case Right(jsonBody) => gson.fromJson(jsonBody, classOf[IndexResponse])
            case Left(message) => IndexResponse("error", message)
        }
        if ("error".equals(resp.status)) throw new SodaClientException(resp.message)
        else resp
    }

    def add(lexicon: String, id: String, names: Array[String], commit: Boolean): AddResponse = {
        val addRequest = AddRequest(lexicon, id, names, commit)
        val request = sttp.body(gson.toJson(addRequest))
            .post(uri"$sodaUrlPrefix/add.json")
        val response = request.send()
        val resp = response.body match {
            case Right(jsonBody) => gson.fromJson(jsonBody, classOf[AddResponse])
            case Left(message) => AddResponse("error", message, addRequest)
        }
        if ("error".equals(resp.status)) throw new SodaClientException(resp.message)
        else resp
    }

    def delete(lexicon: String, id: String): DeleteResponse = {
        val deleteRequest = DeleteRequest(lexicon, id)
        val request = sttp.body(gson.toJson(deleteRequest))
            .post(uri"$sodaUrlPrefix/delete.json")
        val response = request.send()
        val resp = response.body match {
            case Right(jsonBody) => gson.fromJson(jsonBody, classOf[DeleteResponse])
            case Left(message) => DeleteResponse("error", message, deleteRequest)
        }
        if ("error".equals(resp.status)) throw new SodaClientException(resp.message)
        else resp
    }

    def annot(lexicon: String, text: String, matching: String): AnnotResponse = {
        val annotRequest = AnnotRequest(lexicon, text, matching)
        val request = sttp.body(gson.toJson(annotRequest))
            .post(uri"$sodaUrlPrefix/annot.json")
        val response = request.send()
        val resp = response.body match {
            case Right(jsonBody) => gson.fromJson(jsonBody, classOf[AnnotResponse])
            case Left(message) => AnnotResponse("error", message, null)
        }
        if ("error".equals(resp.status)) throw new SodaClientException(resp.message)
        else resp
    }

    def dicts(): DictResponse = {
        val request = sttp.get(uri"$sodaUrlPrefix/dicts.json")
        val response = request.send()
        val resp = response.body match {
            case Right(jsonBody) => gson.fromJson(jsonBody, classOf[DictResponse])
            case Left(message) => DictResponse("error", message, null)
        }
        if ("error".equals(resp.status)) throw new SodaClientException(resp.message)
        else resp
    }

    def coverage(text: String, matching: String): CoverageResponse = {
        val coverageRequest = CoverageRequest(text, matching)
        val request = sttp.body(gson.toJson(coverageRequest))
            .post(uri"$sodaUrlPrefix/coverage.json")
        val response = request.send()
        val resp = response.body match {
            case Right(jsonBody) => gson.fromJson(jsonBody, classOf[CoverageResponse])
            case Left(message) => CoverageResponse("error", message, null)
        }
        if ("error".equals(resp.status)) throw new SodaClientException(resp.message)
        else resp
    }

    def lookup(lexicon: String, id: String): LookupResponse = {
        val lookupRequest = LookupRequest(lexicon, id)
        val request = sttp.body(gson.toJson(lookupRequest))
            .post(uri"$sodaUrlPrefix/lookup.json")
        val response = request.send()
        val resp = response.body match {
            case Right(jsonBody) => gson.fromJson(jsonBody, classOf[LookupResponse])
            case Left(message) => LookupResponse("error", message, null)
        }
        if ("error".equals(resp.status)) throw new SodaClientException(resp.message)
        else resp
    }

    def rlookup(lexicon: String, phrase: String, matching: String): ReverseLookupResponse = {
        val reverseLookupRequest = ReverseLookupRequest(lexicon, phrase, matching)
        val request = sttp.body(gson.toJson(reverseLookupRequest))
            .post(uri"$sodaUrlPrefix/rlookup.json")
        val response = request.send()
        val resp = response.body match {
            case Right(jsonBody) => gson.fromJson(jsonBody, classOf[ReverseLookupResponse])
            case Left(message) => ReverseLookupResponse("error", message, null)
        }
        if ("error".equals(resp.status)) throw new SodaClientException(resp.message)
        else resp
    }
}
