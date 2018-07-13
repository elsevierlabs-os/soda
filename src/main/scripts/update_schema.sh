curl -X POST -H 'Content-type:application/json'  http://localhost:8983/solr/sodaindex/schema -d '{
  "add-field-type":{
    "name" : "tag_exact",
    "class" : "solr.TextField",
    "positionIncrementGap" : "100",
    "postingsFormat" : "FST50",
    "omitTermFreqAndPositions" : true,
    "omitNorms" : true,
    "indexAnalyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters": [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "org.opensextant.solrtexttagger.ConcatenateFilterFactory"}
      ]},
    "queryAnalyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters" : [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"}
      ]
    }
   },
  "add-field-type":{
    "name" : "tag_lower",
    "class" : "solr.TextField",
    "positionIncrementGap" : "100",
    "postingsFormat" : "FST50",
    "omitTermFreqAndPositions" : true,
    "omitNorms" : true,
    "indexAnalyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters":[
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"},
        {"class" : "org.opensextant.solrtexttagger.ConcatenateFilterFactory"}
      ]},
    "queryAnalyzer" : {
      "tokenizer": {
         "class" : "solr.StandardTokenizerFactory" },
      "filters": [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"}
      ]
    }
   },
  "add-field-type":{
    "name" : "tag_stop",
    "class" : "solr.TextField",
    "positionIncrementGap" : "100",
    "postingsFormat" : "FST50",
    "omitTermFreqAndPositions" : true,
    "omitNorms" : true,
    "indexAnalyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters":[
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"},
        {"class" : "solr.StopFilterFactory"},
        {"class" : "org.opensextant.solrtexttagger.ConcatenateFilterFactory"}
      ]},
    "queryAnalyzer" : {
      "tokenizer": {
         "class" : "solr.StandardTokenizerFactory" },
      "filters": [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"},
        {"class" : "solr.StopFilterFactory"}
      ]
    }
   },
  "add-field-type":{
    "name" : "tag_stem1",
    "class" : "solr.TextField",
    "positionIncrementGap" : "100",
    "postingsFormat" : "FST50",
    "omitTermFreqAndPositions" : true,
    "omitNorms" : true,
    "indexAnalyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters" : [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"},
        {"class" : "solr.StopFilterFactory"},
        {"class" : "solr.EnglishMinimalStemFilterFactory"},
        {"class" : "org.opensextant.solrtexttagger.ConcatenateFilterFactory"}
      ]},
    "queryAnalyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters" : [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"},
        {"class" : "solr.StopFilterFactory"},
        {"class" : "solr.EnglishMinimalStemFilterFactory"}      ]
    }
   },
  "add-field-type":{
    "name" : "tag_stem2",
    "class" : "solr.TextField",
    "positionIncrementGap" : "100",
    "postingsFormat" : "FST50",
    "omitTermFreqAndPositions" : true,
    "omitNorms" : true,
    "indexAnalyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters" : [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"},
        {"class" : "solr.StopFilterFactory"},
        {"class" : "solr.KStemFilterFactory"},
        {"class" : "org.opensextant.solrtexttagger.ConcatenateFilterFactory"}
      ]},
    "queryAnalyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters" : [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"},
        {"class" : "solr.StopFilterFactory"},
        {"class" : "solr.KStemFilterFactory"}
      ]
    }
   },
  "add-field-type":{
    "name" : "tag_stem3",
    "class" : "solr.TextField",
    "positionIncrementGap" : "100",
    "postingsFormat" : "FST50",
    "omitTermFreqAndPositions" : true,
    "omitNorms" : true,
    "indexAnalyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters" : [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"},
        {"class" : "solr.StopFilterFactory"},
        {"class" : "solr.PorterStemFilterFactory"},
        {"class" : "org.opensextant.solrtexttagger.ConcatenateFilterFactory"}
      ]},
    "queryAnalyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters" : [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"},
        {"class" : "solr.StopFilterFactory"},
        {"class" : "solr.PorterStemFilterFactory"}
      ]
    }
   },
  "add-field-type":{
    "name" : "phr_exact",
    "class" : "solr.TextField",
    "analyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters": [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"}
      ]
    }
   },
  "add-field-type":{
    "name" : "phr_lower",
    "class" : "solr.TextField",
    "analyzer" : {
      "tokenizer": {
         "class" : "solr.StandardTokenizerFactory" },
      "filters": [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"}
      ]
    }
   },
  "add-field-type":{
    "name" : "phr_stop",
    "class" : "solr.TextField",
    "analyzer" : {
      "tokenizer": {
         "class" : "solr.StandardTokenizerFactory" },
      "filters": [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"},
        {"class" : "solr.StopFilterFactory"}
      ]
    }
   },
  "add-field-type":{
    "name" : "phr_stem1",
    "class" : "solr.TextField",
    "analyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters" : [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"},
        {"class" : "solr.StopFilterFactory"},
        {"class" : "solr.EnglishMinimalStemFilterFactory"}      ]
    }
   },
  "add-field-type":{
    "name" : "phr_stem2",
    "class" : "solr.TextField",
    "analyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters" : [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"},
        {"class" : "solr.StopFilterFactory"},
        {"class" : "solr.KStemFilterFactory"}
      ]
    }
   },
  "add-field-type":{
    "name" : "phr_stem3",
    "class" : "solr.TextField",
    "analyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters" : [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"},
        {"class" : "solr.StopFilterFactory"},
        {"class" : "solr.PorterStemFilterFactory"}
      ]
    }
   },
  "add-field-type":{
    "name" : "phr_esort",
    "class" : "solr.TextField",
    "analyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters": [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"}
      ]}
   },
  "add-field-type":{
    "name" : "phr_s3sort",
    "class" : "solr.TextField",
    "analyzer" : {
      "tokenizer" : {
         "class" : "solr.StandardTokenizerFactory" },
      "filters" : [
        {"class" : "solr.ASCIIFoldingFilterFactory"},
        {"class" : "solr.EnglishPossessiveFilterFactory"},
        {"class" : "solr.LowerCaseFilterFactory"},
        {"class" : "solr.StopFilterFactory"},
        {"class" : "solr.PorterStemFilterFactory"}
      ]}
   },
  "add-field" : { "name" : "lexicon",        "type" : "string" },
  "add-field" : { "name" : "tagname_str",    "type" : "string",     "indexed" : true, "multiValued" : true },
  "add-field" : { "name" : "tagname_exact",  "type" : "tag_exact",  "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "tagname_lower",  "type" : "tag_lower",  "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "tagname_stop",   "type" : "tag_stop",   "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "tagname_stem1",  "type" : "tag_stem1",  "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "tagname_stem2",  "type" : "tag_stem2",  "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "tagname_stem3",  "type" : "tag_stem3",  "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "phrname_exact",  "type" : "phr_exact",  "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "phrname_lower",  "type" : "phr_lower",  "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "phrname_stop",   "type" : "phr_stop",   "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "phrname_stem1",  "type" : "phr_stem1",  "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "phrname_stem2",  "type" : "phr_stem2",  "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "phrname_stem3",  "type" : "phr_stem3",  "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "phrname_esort",  "type" : "phr_esort",  "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "phrname_s3sort", "type" : "phr_s3sort", "stored" : false, "multiValued" : true }
}'
