# IMPORTANT NOTE:
There is a newer and DBpedia Databus compatible version of the DBpedia Lookup here: https://github.com/dbpedia/dbpedia-lookup. The discussion concerning the transition to the new service can be found here: https://forum.dbpedia.org/t/new-dbpedia-lookup-application/607


# DBpedia Lookup

[![Build Status](https://travis-ci.org/dbpedia/lookup.svg?branch=master)](https://travis-ci.org/dbpedia/lookup)

DBpedia Lookup is a web service that can be used to look up DBpedia URIs by related keywords. Related means that either the label of a resource matches, or an anchor text that was frequently used in Wikipedia to refer to a specific resource matches (for example the resource http://dbpedia.org/resource/United_States can be looked up by the string "USA"). The results are ranked by the number of inlinks pointing from other Wikipedia pages at a result page.


## Web APIs

Two APIs are offered: Keyword Search and Prefix Search. A hosted version of the Lookup service is available on the DBpedia server infrastructure.

### Keyword Search

The Keyword Search API can be used to find related DBpedia resources for a given string. The string may consist of a single or multiple words.

Example: Places that have the related keyword "berlin"

http://lookup.dbpedia.org/api/search/KeywordSearch?QueryClass=place&QueryString=berlin

### Prefix Search (i.e. Autocomplete)

The Prefix Search API can be used to implement autocomplete input boxes. For a given partial keyword like *berl* the API returns URIs of related DBpedia resources like http://dbpedia.org/resource/Berlin.

Example: Top five resources for which a keyword starts with "berl"

http://lookup.dbpedia.org/api/search/PrefixSearch?QueryClass=&MaxHits=5&QueryString=berl

### Parameters

The query parameters accepted by the endpoints are

* `QueryString`: a string for which a DBpedia URI should be found.
* `QueryClass`: a DBpedia class from the Ontology that the results should have (for owl#Thing and untyped resource, leave this parameter empty).
* `MaxHits`: the maximum number of returned results (default: 5)

### JSON support

By default all data is returned as XML, the service also retuns JSON to any request including the `Accept: application/json` header.

## Running a local mirror of the webservice

### Clone and build DBpedia Lookup

    git clone git://github.com/dbpedia/lookup.git
    cd lookup
    mvn clean install

### Download and configure the index

You can get our indexes from [HERE](http://downloads.dbpedia-spotlight.org/dbpedia_lookup/)

### Run the server

    
    `./run Server [PATH TO THE INDEX]/[VERSION]/`
   
   E.g:
    
    `./run Server /opt/dbpedia-lookup/2015-04`

**Note: The index file must be decompressed**
    
#### Available versions: 
    
* current - from Latest DBpedia Dump (2015-10)

    
#### Available languages (i18n working in progress): 
    
* en - English
    
    

The server should now be running at http://localhost:1111

## Rebuilding the index

Rebuilding an index is usually not required, if you only intend on running a local mirror of the service you can donwload a prebuilt index as outlined above.

To re-build the index you will require

* DBpedia datasets
* [Wikistatsextractor output](http://downloads.dbpedia-spotlight.org) - [wikistatsextractor](https://github.com/jodaiber/wikistatsextractor) is a drop-in replacement of [pignlproc](https://github.com/dbpedia-spotlight/pignlproc)
* Unix


### Get the following DBpedia datasets
from http://downloads.dbpedia.org/2015-10/core-i18n/en/

* redirects\_en.nt (or .ttl)
* short\_abstracts\_en.nt (or .ttl)
* instance\_types\_en.nt (or .ttl)
* article\_categories\_en.nt (or .ttl)

from http://downloads.dbpedia.org/2015-10/core

* instance_types_en.ttl
* instance_types_sdtyped_dbo_en.ttl
* instance_types_transitive_en.ttl

### Concatenate all data and sort by URI

This is necessary because indexing in sorted order is significantly faster.

      cat instance_types_en.nt (or .ttl)  \
          short_abstracts_en.nt (or .ttl) \
          article_categories_en.nt (or .ttl) \
          instance_types_en.ttl  \
          instance_types_sdtyped_dbo_en.ttl \
          instance_types_transitive_en.ttl | sort >all_dbpedia_data.nt (or .ttl)

### Get the dataset redirects\_en.nt (or .ttl)

Redirects are not indexed, but they are excluded as targets of lookup.

### Run Indexer

The indexer has to be run twice:

1. with the DBpedia data 

        ./run Indexer lookup_index_dir redirects_en.nt (or .ttl) all_dbpedia_data.nt (or .ttl)

2. with the wikistatsextractor data

        ./run Indexer lookup_index_dir redirects_en.nt (or .ttl) pairCounts

## Support and feedback

The best way to get support or give feedback on the Lookup project is via the [DBpedia discussion mailing list](https://lists.sourceforge.net/lists/listinfo/dbpedia-discussion). More technical queries about the code base should be directed to the [DBpedia developers mailing list](https://lists.sourceforge.net/lists/listinfo/dbpedia-developers).

The [DBpedia wiki](http://wiki.dbpedia.org/lookup/) also has useful information on the project.

## Maintainers

* Kunal Jha [@Kunal-Jha](https://github.com/Kunal-Jha)
* Sandro Coelho [@sandroacoelho](https://github.com/sandroacoelho)
* Pablo Mendes [@pablomendes](https://github.com/pablomendes) (less active)
* Max Jakob [@maxjakob](https://github.com/maxjakob) (less active)
* Matt Haynes [@matth](https://github.com/matth) (less active)
