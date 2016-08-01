# DBpedia Lookup

[![Build Status](https://travis-ci.org/dbpedia/lookup.svg?branch=master)](https://travis-ci.org/dbpedia/lookup)

DBpedia Lookup is a web service that can be used to look up DBpedia URIs by related keywords. Related means that either the label of a resource matches, or an anchor text that was frequently used in Wikipedia to refer to a specific resource matches (for example the resource http://dbpedia.org/resource/United_States can be looked up by the string "USA"). The results are ranked by the number of inlinks pointing from other Wikipedia pages at a result page.

## Web APIs

Two APIs are offered: Keyword Search and Prefix Search. A hosted version of the Lookup service is available on the DBpedia server infrastructure.

### Keyword Search

The Keyword Search API can be used to find related DBpedia resources for a given string. The string may consist of a single or multiple words.

Example: Places that have the related keyword "berlin"

http://lookup.dbpedia.org/api/search/KeywordSearch?QueryClass=place&QueryString=berlin&&Lang=de

### Prefix Search (i.e. Autocomplete)

The Prefix Search API can be used to implement autocomplete input boxes. For a given partial keyword like *berl* the API returns URIs of related DBpedia resources like http://dbpedia.org/resource/Berlin.

Example: Top five resources for which a keyword starts with "berl"

http://lookup.dbpedia.org/api/search/PrefixSearch?QueryClass=&MaxHits=5&QueryString=berl&&Lang=de

### Parameters

The query parameters accepted by the endpoints are

* `QueryString`: a string for which a DBpedia URI should be found.
* `QueryClass`: a DBpedia class from the Ontology that the results should have (for owl#Thing and untyped resource, leave this parameter empty).
* `MaxHits`: the maximum number of returned results (default: 5)
* `Language`: the language of indexer to be used for searching the query (default: en)


### JSON and Google KG like ouptut Support

By default all data is returned as XML, the service also retuns JSON to any request including the `Accept: application/json` header. Google Knowledge Graph like output can also be obtained `Accept: application/json+ld`

## Running a local mirror of the webservice

There are two ways of running the service:

1. Docker - Using docker is an easy way to run the lookup without any pre-requisite tool installed in the machine.

2. Standard method - In order to follow these steps please make sure you have Java and/or Scala SDK and Maven installed on your machine.


### 1. Using Docker
1. Downlaod the latest version of [Docker](https://www.docker.com/products/overview).
2. Once the docker is installed, run the following commands:

     `docker pull dbpedia/lookup`


     `docker run -p 1111:1111 lookup`



### 2. Standard Method :Clone and build DBpedia Lookup

    git clone git://github.com/dbpedia/lookup.git
    cd lookup
    mvn clean install

### Download and configure the index

You can get our i18n supposted indexes from [Server](http://downloads.dbpedia.org/temporary/lookup/)

### Run the server


    `./run Server [PATH TO THE INDEXBASEDIR]/`

   E.g:

    `./run Server /opt/dbpedia-lookup/index`


**Note: Please maintain the structure of the index folder in order for the code to work accurately. The index file must be decompressed**

#### Available versions:

* current - from Latest DBpedia Dump

Archive: 2015-04, 3.9 and 3.8


#### Available languages:

* en - English
* es - Spanish
* de - German


The server should now be running at http://localhost:1111

## Updating the Documentation

The documentation is controlled with the swagger.yaml file. The swagger file contains the descriptions of the services and the models present inside the lookup code. Once this file is updated along with the code, re building the whole project would result in generation of static html pages in target/generatedSwaggerDocument folder.

In order to look into the code flow and module descriptions copy the swagger.yaml file and paste into the [Swagger editor](http://editor.swagger.io/#/).

## Rebuilding the index

Rebuilding an index is usually not required, if you only intend on running a local mirror of the service you can donwload a prebuilt index as outlined above.

To re-build the index you will require

* DBpedia datasets
* [Wikistatsextractor output](http://spotlight.sztaki.hu/downloads/latest_data/) - [wikistatsextractor](https://github.com/jodaiber/wikistatsextractor) is a drop-in replacement of [pignlproc](https://github.com/dbpedia-spotlight/pignlproc)
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

2. with the wikistatsextractor data

        ./run Indexer lookup_index_dir redirects_en.nt pairCounts

### Incorporating the new Index
1. Rename the folder containing your index in the format index\_[language]. For example, index\_en.
2. Copy the new index to the folder where all the other indexes (downloaded/generated) are stored.
3. Open the file dbpedia.properties  (lookup/src/main/resources/config/dbpedia.properties).
4. Add a new property index\_[lang]="index\_[language]".
5. Finally update the switch cases to point to the new property for the indexed language in the Searcher.scala module under the lucene package.

## Support and feedback

The best way to get support or give feedback on the Lookup project is via the [DBpedia discussion mailing list](https://lists.sourceforge.net/lists/listinfo/dbpedia-discussion). More technical queries about the code base should be directed to the [DBpedia developers mailing list](https://lists.sourceforge.net/lists/listinfo/dbpedia-developers).

The [DBpedia wiki](http://wiki.dbpedia.org/lookup/) also has useful information on the project.

## Maintainers

* Pablo Mendes [@pablomendes](https://github.com/pablomendes)
* Matt Haynes [@matth](https://github.com/matth)
