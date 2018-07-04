## SoDA API

This document describes the SoDA API. The SoDA API uses JSON over HTTP and is thus language agnostic. A SoDA request is built as a JSON document and is sent to the SoDA JSON endpoint over HTTP POST. SoDA responds with another JSON document indicating success or failure.

### Table of Contents

- [Index](#index)
- [Add Lexicon Entries](#add-lexicon-entries)
- [Delete Lexicon (Entries)](#delete-lexicon-entries)
- [Annotate](#annotate)
- [List Lexicons](#list-lexicons)
- [Coverage Info](#coverage-info)
- [Lookup Dictionary Entry](#lookup-dictionary-entry)

----

The following section provides details about each of the endpoints.

### Index

__INPUT__
This just returns a status OK JSON message. It is meant to test if the SoDA web component is alive.

__URL__ http://host:port/soda/index.json

__INPUT__

None

__OUTPUT__

````json
    {
        "status": "ok",
        "message": "SoDA accepting requests (Solr version 7.3.1)"
    }
````

__EXAMPLE PYTHON CLIENT__

````python
    import sodaclient

    client = sodaclient.SodaClient("http://host:port/soda")
    resp = client.index()
````

__EXAMPLE SCALA CLIENT__

````scala
    import com.elsevier.soda.messages._
    import com.elsevier.soda.SodaClient

    val client = new SodaClient("http://host:port/soda")
    val resp: IndexResponse = client.index()
````

----

### Add Lexicon Entries

Adds new entries to a named Lexicon.

__URL__ http://host:port/soda/add.json

__INPUT__

````json
    {
        "lexicon" : "lexicon_name", 
        "id" : "unique_url_of_entry",
        "names" : ["name_1", "name_2", "name_3"],
        "commit" : true
    }
````

The id value we have chosen to use is the RDF URI of the entity as reported in the imported lexicon. The names are the strings to match for that entity, a single entry can have multiple names. The commit is optional, if omitted, each addition operation results in a commit, which is inefficient. It is better to either commit at regular intervals, and once at the end. In order to send a commit request using the save.json endpoint, omit the id and names entries, like this:

````json
    {
        "lexicon" : "countries", 
        "commit" : true
    }
````

__OUTPUT__

````json
    {
        "status": "ok",
        "payload": {
            "lexicon" : "countries", 
            "id" : "http://www.geonames.org/CHN",
            "names" : ["China", "Chine", "CHN"],
            "commit" : true
        }
    }
````

__EXAMPLE PYTHON CLIENT__

```python
    import sodaclient

    client = sodaclient.SodaClient("http://host:port/soda")
    resp = client.add(lexicon_name, id, names, commit)
```

__EXAMPLE SCALA CLIENT__

```scala
    import com.elsevier.soda.messages._
    import com.elsevier.soda.SodaClient

    val client = new SodaClient("http://host:port/soda")
    val resp: AddResponse = client.add(lexiconName, id, names, commit)
```
----

### Delete Lexicon

A single SoDA index can contain entries from multiple lexicons. This operation deletes all entries in a Lexicon, or a single ID if an ID is specified in the request.

__URL__ http://host:port/soda/delete.json


__INPUT__

````json
    { 
        "lexicon" : "countries",
        "id": "http://www.geonames.org/CHN"
    }
````

__OUTPUT__

````json
    {
        "status": "ok",
        "payload": {
            "lexicon" : "countries",
            "id": "http://www.geonames.org/CHN"
        }
    }
````

__EXAMPLE PYTHON CLIENT__

````python
    import sodaclient

    client = sodaclient.SodaClient("http://host:port/soda")
    resp = client.delete(lexicon_name, optional_id)
````

__EXAMPLE SCALA CLIENT__

```scala
    import com.elsevier.soda.messages._
    import com.elsevier.soda.SodaClient

    val client = new SodaClient("http://host:port/soda")
    val resp: DeleteResponse = client.delete(lexiconName, optionalID)
```

----

### Annotate Document

Annotates text against a specified lexicon and match type. Match type can be one of the following.

* __exact__ - matches text against the FST maintained in Solr by SolrTextTagger. This will match segments in text that are identical to a dictionary entry.
* __lower__ - same as exact, but matches are now case insensitive.
* __stop__ - same as lower, but with standard English stopwords removed.
* __stem1__ - same as stop, but with the Solr Minimal English stemmer applied.
* __stem2__ - same as stop, but with the KStem stemmer applied.
* __stem3__ - same as stop, but with the Porter stemmer applied.

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
    {
        "status": "ok",
        "annotations": [
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
    }
````

__EXAMPLE PYTHON CLIENT__

````python
    import sodaclient

    client = sodaclient.SodaClient("http://host:port/soda")
    resp = client.annot(lexicon_name, text, matching)
````

__EXAMPLE SCALA CLIENT__

````scala
    import com.elsevier.soda.messages._
    import com.elsevier.soda.SodaClient

    val client = new SodaClient("http://host:port/soda")
    val resp: AnnotResponse = client.annot(lexicon, text, matching)
````

### List Lexicons

Returns a list of lexicons available to annotate against. Currently we only allow the ability to annotate documents against a single lexicon. When requiring annotations against multiple documents, it is recommended to annotate documents separately against each lexicon, then merge the annotations.

__URL__ http://host:port/soda/dicts.json

__INPUT__

None

__OUTPUT__

````json
    {
        "status": "ok",
        "lexicons": [
            {
                "lexicon": "countries",
                "count": 2
            }
        ]
    }
````

__EXAMPLE PYTHON CLIENT__

````python
    import sodaclient

    client = sodaclient.SodaClient("http://host:port/soda")
    resp = client.dicts()
````

__EXAMPLE SCALA CLIENT__

````scala
    import com.elsevier.soda.messages._
    import com.elsevier.soda.SodaClient

    val client = new SodaClient()
    val resp: DictResponse = client.dicts()
````

----

### Coverage Info

This can be used to find which lexicons are appropriate for annotating your text. The service allows you to send a piece of text to all hosted lexicons and returns with the number of matches found in each.

__URL__ http://host:port/soda/coverage.json

__INPUT__

````json
    {
        "text" : "Institute of Clean Coal Technology, East China University of Science and Technology, Shanghai 200237, China",
        "matching": "exact"
    }
````

__OUTPUT__

````json
    {
        "status": "ok",
        "lexicons": [
            "lexicon": "countries",
            "count": 2
        ]
    }
````

__EXAMPLE PYTHON CLIENT__

````python
    import sodaclient

    client = sodaclient.SodaClient("http://host:port/soda")
    resp = client.coverage(text, matching)
````

__EXAMPLE SCALA CLIENT__

````scala
    import com.elsevier.soda.messages._
    import com.elsevier.soda.SodaClient

    val client = new SodaClient("http://host:port/soda")
    val resp: CoverageResponse = client.coverage(text, matching)
````

----

### Lookup Dictionary Entry

This service allows client to look up a dictionary entry from the index by lexicon and ID.

__URL__ http://host:port/lookup.json

__INPUT__
````json
    {
        "lexicon": "countries",
        "id": "http://www.geonames.org/CHN"
    }
````

__OUTPUT__
````json
    {
        "status": "ok",
        "entries": [
            {
                "lexicon": "countries",
                "id": "http://www.geonames.org/CHN",
                "names": [ "China", "Chine", "CHN" ]
            }
        ]
    }
````

__EXAMPLE_PYTHON_CLIENT__

````python
    import sodaclient

    client = sodaclient.SodaClient("http://host:port/soda")
    resp = client.lookup(lexicon, id)
````

__EXAMPLE_SCALA_CLIENT__

````scala
    import com.elsevier.soda.messages._
    import com.elsevier.soda.SodaClient

    val client = new SodaClient("http://host:port/soda")
    val resp: LookupResponse = client.lookup(lexicon, id)
````

----
