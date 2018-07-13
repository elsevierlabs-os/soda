package com.elsevier.soda.messages

case class ReverseLookupEntry (id: String, lexicon: String, text: String, confidence: Double)

case class ReverseLookupResponse (status: String, message: String, entries: Array[ReverseLookupEntry])
