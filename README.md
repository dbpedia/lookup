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

### Clone and build the DBpedia extraction framework
DBpedia Lookup depends on the core of the DBpedia extraction framework, which is not available in a public Maven repo at the moment. Java 7 is required to compile it.
    
    git clone git://github.com/dbpedia/extraction-framework.git
    cd extraction-framework
    git checkout DBpedia_3.8
    mvn clean install

### Clone and build DBpedia Lookup

    git clone git://github.com/dbpedia/lookup.git
    cd lookup
    mvn clean install

### Download and configure the index

    You can get our indexes from [SourceForge](https://sourceforge.net/projects/dbpedia-lookup/files/index/)

### Run the server

    
    `./run Server dbpedia-lookup-index/[LANGUAGE]/[VERSION]/core-i18n`
   
   E.g:
    
    `./run Server dbpedia-lookup-index/current/en/core-i18n`
    
    
#### Available versions: 
    
* current - from Latest DBpedia Dump

Archive: 2015-04, 3.9 and 3.8 
    
    
#### Available languages (i18n working in progress): 
    
* en - English
    
    

The server should now be running at http://localhost:1111

## Rebuilding the index

Rebuilding an index is usually not required, if you only intend on running a local mirror of the service you can donwload a prebuilt index as outlined above.

To re-build the index you will require

* DBpedia datasets
* pignlproc output from [nerd-stats.pig](https://github.com/dbpedia-spotlight/pignlproc/blob/master/examples/nerd-stats/nerd-stats.pig)
* Unix


### Get the following DBpedia datasets
from http://downloads.dbpedia.org/current/en/

* redirects\_en.nt
* short\_abstracts\_en.nt
* instance\_types\_en.nt
* article\_categories\_en.nt

### Concatenate all data and sort by URI

This is necessary because indexing in sorted order is significantly faster.

      cat instance_types_en.nt  \
          short_abstracts_en.nt \
          article_categories_en.nt | sort >all_dbpedia_data.nt

### Get the dataset redirects\_en.nt

Redirects are not indexed, but they are excluded as targets of lookup.

### Run Indexer

The indexer has to be run twice:

1. with the DBpedia data 

        ./run Indexer lookup_index_dir redirects_en.nt all_dbpedia_data.nt

2. with the pignlproc data

        ./run Indexer lookup_index_dir redirects_en.nt nerd_stats_output.tsv

## Support and feedback

The best way to get support or give feedback on the Lookup project is via the [DBpedia discussion mailing list](https://lists.sourceforge.net/lists/listinfo/dbpedia-discussion). More technical queries about the code base should be directed to the [DBpedia developers mailing list](https://lists.sourceforge.net/lists/listinfo/dbpedia-developers).

The [DBpedia wiki](http://wiki.dbpedia.org/lookup/) also has useful information on the project.

## Maintainers

* Pablo Mendes [@pablomendes](https://github.com/pablomendes)
* Max Jakob [@maxjakob](https://github.com/maxjakob)
* Matt Haynes [@matth](https://github.com/matth)

