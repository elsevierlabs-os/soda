package com.elsevier.soda.messages

case class LookupEntry (lexicon: String, id: String, names: Array[String])

case class LookupResponse (status: String, message: String, entries: Array[LookupEntry])
