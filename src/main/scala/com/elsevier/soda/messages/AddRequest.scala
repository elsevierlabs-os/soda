package com.elsevier.soda.messages

case class AddRequest(lexicon: String,
                      id: String,
                      names: Array[String],
                      commit: Boolean)
