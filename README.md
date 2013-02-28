# DBpedia Lookup

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

### Clone and build the project

    git clone git://github.com/dbpedia/lookup.git
    cd lookup
    mvn clean install

### Download and configure the index

    wget http://spotlight.dbpedia.org/download/lookup_index.tgz
    tar zxvf lookup_index.tgz
    echo "./network/www5/dbpedia-lookup/lookup_index" > default_index_path

### Run the server

    mvn scala:run

The server should now be running at http://localhost:1111

## Rebuilding the index

Rebuilding an index is usually not required, if you only intend on running a local mirror of the service you can donwload a prebuilt index as outlined above.

To re-build the index you will require ..

* DBpedia datasets
* Counts of Wikipedia inlinks for all resources
* Set of surface forms for resources
* Unix

### Get a file with Refcounts

Count the number of Wikipedia page inlinks for a resource. This number is required for ranking. Store them in N-Triples format with the predicate http://dbpedia.org/property/refCount, for example:

      <http://dbpedia.org/resource/ABBA> 
      <http://dbpedia.org/property/refCount> 
      "1234"^^<http://www.w3.org/2001/XMLSchema#integer> .

### Get a list of surface forms for a resource

Take for example anchor links of wiki page links that point to a resource, but only take the ones that appear X times or more often (X should be at least 30 or 50). Store them in N-Triples format with the predicate http://lexvo.org/ontology#label, for example:

      <http://dbpedia.org/resource/United_States> 
      <http://lexvo.org/ontology#label>
      "USA"@en .

### Get the following DBpedia datasets

* short\_abstracts\_en.nt
* instance\_types\_en.nt
* article\_categories\_en.nt

### Concatenate all data and sort by URI

This is necessary because indexing in sorted order is significantly faster.

Note: if the input is not sorted the index will be not as expected

      cat ref_counts.nt         \
          surface_forms.nt      \
          instance_types_en.nt  \
          short_abstracts_en.nt \
          article_categories_en.nt | sort >data-to-be-indexed.nt

### Get the dataset redirects\_en.nt

Redirects are not indexed, but they are excluded as targets of lookup.

### Run Indexer with three arguments

1. target Lucene directory (create an empty one if necessary),
  e.g. 'c:\lucene\_lookup\_index'
2. redirects dataset,
  e.g. 'c:\redirects\_en.nt'
3. concatenated and sorted data,
  e.g. 'c:\data-to-be-indexed.nt'


      mvn scala:run -Dlauncher=Indexer "-DaddArgs=c:\lucene_lookup_index|c:\redirects_en.nt|c:\data-to-be-indexed.nt"

## Support and feedback

The best way to get support or give feedback on the Lookup project is via the [DBpedia discussion mailing list](https://lists.sourceforge.net/lists/listinfo/dbpedia-discussion). More technical queries about the code base should be directed to the [DBpedia developers mailing list](https://lists.sourceforge.net/lists/listinfo/dbpedia-developers).

The [DBpedia wiki](http://wiki.dbpedia.org/lookup/) also has useful information on the project.

## Team

### Maintainers

[Pablo Mendes](http://www.wiwiss.fu-berlin.de/en/institute/pwo/bizer/team/MendesPablo.html) (Freie Universität Berlin)

[Max Jakob](http://www.wiwiss.fu-berlin.de/en/institute/pwo/bizer/team/JakobMax.html) (Freie Universität Berlin)

