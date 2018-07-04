## Changes in this release

### Software version changes

* Solr version moved from 5.0.0 to 7.3.1
* SolrTextTagger version moved from 2.1-SNAPSHOT to 2.6-SNAPSHOT
* Scala version moved from 2.10.6 to 2.12.6
* SBT version moved from 0.13 to 1.0

### Functionality changes

* Fuzzy matching implementation using OpenNLP phrase chunking has been removed since it was not used and did not work well.
* Three additional stemming matches introduced, using Solr's Minimal English Stemmer, KStem stemmer and Porter stemmer. Unlike the old fuzzy matching implementation, these work the same way (streaming) as the original exact and lower matches.
* API changes made to standardize response payloads across services, and across success and error responses, making error handling for clients simpler.
* General cleanup, removing unnecessary libraries, standardizing JSON parsing and generation using Google's GSON library, and standardizing HTTP access using the new Scala sttp library.
* Addition of an importable Python client sodaclient that mimics the API for the existing Scala client SodaClient.
* Addition of a generic (single-threaded) dictionary loader.
* Installation is now considerably simplified, using the new Solr JSON API.

### Other changes

* SolrTextTagger now uses the FST Postings format instead of the Memory Postings format, which is marginally slower (see discussion on [SolrTextTagger Issue#38](https://github.com/OpenSextant/SolrTextTagger/issues/38)) but has the advantage in that the size of the dictionary is no longer bound by the size of the JVM heap allocated to Solr.


