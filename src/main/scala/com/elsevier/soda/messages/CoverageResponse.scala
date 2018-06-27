package com.elsevier.soda.messages

case class Coverage (lexicon: String, count: Long)

case class CoverageResponse (status: String, message: String, lexicons: Array[Coverage])

