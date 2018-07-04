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
  "add-field" : { "name" : "lexicon",       "type" : "string" },
  "add-field" : { "name" : "tagname_str",  "type" : "string",    "indexed" : true, "multiValued" : true },
  "add-field" : { "name" : "tagname_exact", "type" : "tag_exact", "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "tagname_lower", "type" : "tag_lower", "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "tagname_stop",  "type" : "tag_stop",  "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "tagname_stem1", "type" : "tag_stem1", "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "tagname_stem2", "type" : "tag_stem2", "stored" : false, "multiValued" : true },
  "add-field" : { "name" : "tagname_stem3", "type" : "tag_stem3", "stored" : false, "multiValued" : true }
}'
