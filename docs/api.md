## SoDA API

This document describes the SoDA API. The SoDA API uses JSON over HTTP and is thus language agnostic. A SoDA request is built as a JSON document and is sent to the SoDA JSON endpoint over HTTP POST. SoDA responds with another JSON document indicating success or failure.

### Table of Contents

- [Index](#index)
- [List Lexicons](#list-lexicons)
- [Annotate Document](#annotate-document)
- [Delete Lexicon](#delete-lexicon)
- [Add Lexicon Entries](#add-lexicon-entries)
- [Coverage Info](#coverage-info)

(_generated with [DocToc](http://doctoc.herokuapp.com/)_)

----

The following section provides details about each of the endpoints.

### Index

This just returns a status OK JSON message. It is meant to test if the SoDA web component is alive.

__URL__ http://host:port/soda/index.json

__INPUT__

None

__OUTPUT__

````json
    { "status": "ok" }
````

__EXAMPLE PYTHON CLIENT__

````python
    import json
    import requests

    resp = requests.get("http://host:port/soda/index.json")
    print json.loads(resp.text)
````

__EXAMPLE SCALA CLIENT__

````scala
    import com.elsevier.soda.SodaClient

    val sodaClient = new SodaClient()
    val resp = sodaClient.get("http://host:port/soda/index.json")
    Console.println(resp)
````

----

### List Lexicons

Returns a list of lexicons available to annotate against. Currently we only allow the ability to annotate documents against a single lexicon. When requiring annotations against multiple documents, it is recommended to annotate documents separately against each lexicon, then merge the annotations.

__URL__ http://host:port/soda/dicts.json

__INPUT__

None

__OUTPUT__

````json
    [
        { "lexicon" : "countries", "numEntries" : 248 }
    ]
````

__EXAMPLE PYTHON CLIENT__

````python
    import requests

    resp = requests.get("http://host:port/soda/dicts.json")
    print json.loads(resp.text)
````

__EXAMPLE SCALA CLIENT__

````scala
    import com.elsevier.soda.SodaClient

    val sodaClient = new SodaClient()
    val resp = sodaClient.get("http://host:port/soda/dicts.json")
    Console.println(resp)
````

----

### Annotate Document

Annotates text against a specified lexicon and match type. Match type can be one of the following.

* __exact__ - matches text against the FST maintained in Solr by SolrTextTagger. This will match segments in text that are identical to a dictionary entry.
* __lower__ - same as exact, but matches are now case insensitive.
* __punct__ - removes punctuation and phrase chunks input, then matches phrases against lexicon entries. Also case insensitive.
* __sort__ - same as punct, except that words in phrase chunks are sorted and matched against similarly sorted lexicon entries.
* __stem__ - same as sort, except words in phrases are stemmed using Porter stemmer and matched against similarly stemmed lexicon entries.

__URL__ http://host:port/soda/annot.json

__INPUT__

````json
    {
        "lexicon" : "countries",
        "text" : "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China",
        "matching" : "exact"
    }
````

__OUTPUT__

````json
    [
        {
            "id" : "http://www.geonames.org/CHN", 
            "lexicon" : "countries", 
            "begin" : 41, 
            "end" : 46,
            "coveredText" : "China", 
            "confidence" : "1.0"
        }, 
        {
            "id" : "http://www.geonames.org/CHN", 
            "lexicon" : "countries",
            "begin" : 102, 
            "end" : 107,
            "coveredText" : "China", 
            "confidence" : "1.0"
        }
    ]
````

__EXAMPLE PYTHON CLIENT__

````python
    import json
    import requests

    params = {
        "lexicon" : "countries",
        "text" : "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China",
        "matching" : "exact"
    }
    req = json.dumps(params)
    resp = requests.post("http://host:port/soda/annot.json", data=req)
    print json.loads(resp.text)
````

__EXAMPLE SCALA CLIENT__

````scala
    import com.elsevier.soda.{SodaClient, SodaUtils}

    val sodaClient = new SodaClient()
    val req = SodaUtils.jsonBuild(Map(
        "lexicon" -> "countries",
        "text" -> "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China",
        "matching" -> "exact"))
    val resp = sodaClient.post("http://host:port/soda/annot.json", req)
    Console.println(SodaUtils.jsonParseList(resp))
````

----

### Delete Lexicon

A single SoDA index can contain entries from multiple lexicons. This operation deletes all entries in a Lexicon.

__URL__ http://host:port/soda/delete.json


__INPUT__

````json
    { "lexicon" : "lexicon_name" }
````

__OUTPUT__

````json
    {"status": "ok"}
````

__EXAMPLE PYTHON CLIENT__

````python
    import json
    import requests

    params = { "lexicon" : "countries" }
    req = json.dumps(params)
    resp = requests.post("http://host:port/soda/delete.json", data=req)
    print json.loads(resp.text)
````

__EXAMPLE SCALA CLIENT__

```scala
    import com.elsevier.soda.{SodaClient, SodaUtils}

    val sodaClient = new SodaClient()
    val params = Map("lexicon" -> "countries")
    val req = SodaUtils.jsonBuild(params)
    val resp = sodaClient.post("http://host:port/soda/delete.json", req)
    Console.println(SodaUtils.jsonParse(resp))
```

----

### Add Lexicon Entries

Adds new entries to a named Lexicon.

__URL__ http://host:port/soda/save.json

__INPUT__

````json
    {
        "lexicon" : "lexicon_name", 
        "id" : "unique_url_of_entry",
        "names" : ["name_1", "name_2", "name_3"],
        "commit" : true_or_false
    }
````

The id value we have chosen to use is the RDF URI of the entity as reported in the imported lexicon. The names are the strings to match for that entity, a single entry can have multiple names. The commit is optional, if omitted, each addition operation results in a commit, which is inefficient. It is better to either commit at regular intervals, and once at the end. In order to send a commit request using the save.json endpoint, omit the id and names entries, like this:

````json
    {
        "lexicon" : "lexicon_name", 
        "commit" : true
    }
````

__OUTPUT__

````json
    {"status": "ok"}
````

__EXAMPLE PYTHON CLIENT__

```python
    import json
    import requests

    params = {
        "lexicon" : "countries",
        "id" : "http://www.geonames.org/AND",
        "names" : ["AND", "Andorra", "Andorre"],
        "commit" : false
    }
    req = json.dumps(params)
    resp = requests.post("http://host:port/soda/add.json", data=req)
    print json.loads(resp.text)
```

__EXAMPLE SCALA CLIENT__

```scala
    import com.elsevier.soda.{SodaClient, SodaUtils}

    val sodaClient = new SodaClient()
    val params = Map(
        "lexicon" -> "countries",
        "id" -> "http://www.geonames.org/AND",
        "names" -> List("AND", "Andorra", "Andorre"),
        "commit" -> false
    )
    val req = SodaUtils.jsonBuild(params)
    val resp = sodaClient.post("http://host:port/soda/add.json", req)
    Console.println(SodaUtils.jsonParse(resp))
```

----

### Coverage Info

This can be used to find which lexicons are appropriate for annotating your text. The service allows you to send a piece of text to all hosted lexicons and returns with the number of matches found in each.

__URL__ http://host:port/soda/coverage.json

__INPUT__

````json
    { "text" : "the text to annotate" }
````

__OUTPUT__

````json
    [
        { "lexicon" : "lexicon_name", "numEntries" : 10 },
        { "lexicon" : "another_lexicon", "numEntries" : 100 }
    ]
````

__EXAMPLE PYTHON CLIENT__

````python
    import json
    import requests

    params = { "text" : "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China" }
    req = json.dumps(params)
    resp = requests.post("http://host:port/soda/coverage.json", req)
    print json.loads(resp.text)
````

__EXAMPLE SCALA CLIENT__

````scala
    import com.elsevier.soda.{SodaClient, SodaUtils}

    val sodaClient = new SodaClient()
    val req = SodaUtils.jsonBuild(Map(
        "text" -> "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China"
    ))
    val resp = sodaClient.post("http://host:port/soda/coverage.json", req)
    Console.println(SodaUtils.jsonParseList(resp))
````
