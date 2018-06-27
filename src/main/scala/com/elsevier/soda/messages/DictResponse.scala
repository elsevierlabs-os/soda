package com.elsevier.soda.messages

case class DictCountPair (lexicon: String, count: Long)

case class DictResponse(status: String, message: String, lexicons: Array[DictCountPair])
