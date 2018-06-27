package com.elsevier.soda.messages

case class Annotation(id: String,
                      lexicon: String,
                      begin: Int,
                      end: Int,
                      coveredText: String,
                      confidence: Double)

case class AnnotResponse(status: String, message: String, annotations: Array[Annotation])