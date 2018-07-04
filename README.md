## Solr Dictionary Annotator

### Introduction

The Solr Dictionary Annotator (SoDA) is a Dictionary-based Annotator (or Gazetteer) that supports exact as well as fuzzy lookups across multiple lexicons.

SoDA is backed by a Solr index which holds entity names (primary and alternate names), as well as an identifier for that entity. Multiple copies of these entity names, stemmed by a set of stemming algorithms of various strengths, are created and stored in the index. During annotation, the text to be annotated is stemmed and spans matched against similarly stemmed entity names in the index. Fast (FST based) span lookup is done using the [SolrTextTagger](https://github.com/OpenSextant/SolrTextTagger) project.

SoDA supports multiple dictionaries (lexicons) within the same Solr index. Matching modes currently supported are exact, lower (case insensitive), stop (english stopwords removed), and three levels of stemming (stem1, stem2, stem3) implemented using Solr's Minimal English Stemmer, KStem stemmer and Porter Stemmer respectively.

### Usage

SoDA provides a JSON over HTTP interface. Requests are submitted to SoDA as JSON documents over HTTP POST, and SoDA responds with JSON documents. This form of API allows us to be language agnostic and cross platform. In addition, SoDA also provides a Scala and a Python client, both of which expose a programmatic interface to SoDA.

Because of the language and platform independence, SoDA can be accessed from other event sources as well, such as [Apache Spark](https://spark.apache.org/) or [Databricks Notebook](https://databricks.com/product/databricks-cloud) environments using Python and Scala.

### Architecture

In terms of architecture, the SoDA system looks something like this. SoDA itself is a fairly lightweight application, and while this is not necessary, it can generally co-exist with Solr on the same box.

![Architecture](docs/architecture.png)

_Fig 1: SoDA architecture_

### More Information

* [Changes in this release](docs/changelog-v2.md)
* [SoDA Installation and Configuration](docs/installation.md)
* [SoDA Application Programming Interface (API)](docs/api.md) 

#### SoDA v1.0 links

The following links describe SoDA v1, which used an older version of Solr (5.0.0) and SolrTextTagger (2.1-SNAPSHOT). The latest release of SoDA v1 can be retrieved using the tag "v1.1". The major difference between v1 and v2 is that the OpenNLP phrase based fuzzy matching has been replaced with multiple levels of stemmed matching, see [Issue#12](https://github.com/elsevierlabs-os/soda/issues/12) for the discussion. Regrettably, I don't have the bandwidth to support v1, please consider moving to the latest version.

* [Running SoDA from Docker (v1)](docs/docker-setup-v1.md)
* [SoDA Installation and Configuration (v1)](docs/installation-v1.md)
* [SoDA Application Programming Interface (v1)](docs/api-v1.md) 

### Citing

If you need to cite SoDA in your work, please use the following DOI:

[![DOI](https://zenodo.org/badge/21245/elsevierlabs-os/soda.svg)](https://zenodo.org/badge/latestdoi/21245/elsevierlabs-os/soda) 

Pal, Sujit (2015). Solr Dictionary Annotator [Computer Software]; https://github.com/elsevierlabs-os/soda


