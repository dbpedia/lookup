                          ********************
                             DBpedia Lookup
                          ********************

Author: Max Jakob (max.jakob@fu-berlin.de)
Date: 2011-02-23

The DBpedia Lookup Service can be used to look up DBpedia URIs by related
keywords. Related means that either the label of a resource matches, or
an anchor text that was frequently used in Wikipedia to refer to a specific
resource matches (for example the resource
http://dbpedia.org/resource/United_States can be looked up by the string
"USA"). The results are ranked by the number of inlinks pointing from other
Wikipedia pages at a result page.

http://lookup.dbpedia.org/
http://dbpedia.org/lookup


Requirements:
~~~~~~~~~~~~~
- Maven

To re-build the index:
- DBpedia datasets
- Counts of Wikipedia inlinks for all resources
- Set of surface forms for resources
- Unix


Specify Lucene index directory
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The path to the Lucene index can be specified in the first line of the file
  default_index_path


Run DBpedia Lookup Service Server:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
mvn scala:run

Server will run at http://localhost:1111/ .


How to re-build the Lucene index:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1. Get a file with Refcounts: count the number of Wikipedia page inlinks
   for a resource. This number is required for ranking.
   Store them in N-Triples format with the predicate
   http://dbpedia.org/property/refCount, for example
      <http://dbpedia.org/resource/ABBA> 
      <http://dbpedia.org/property/refCount> 
      "1234"^^<http://www.w3.org/2001/XMLSchema#integer> .

2. Get a list of surface forms for a resource: take for example anchor
   links of wiki page links that point to a resource, but only take the
   ones that appear X times or more often (X should be at least 30 or 50).
   Store them in N-Triples format with the predicate
   http://lexvo.org/ontology#label, for example
      <http://dbpedia.org/resource/United_States> 
      <http://lexvo.org/ontology#label>
      "USA"@en .

3. Get the following DBpedia datasets:
   - short_abstracts_en.nt
   - instance_types_en.nt
   - article_categories_en.nt

4. Concatenate all data and sort by URI. This is necessary because indexing
   in sorted order is significantly faster.
   !! Note: if the input is not sorted the index will be not as expected !!
      cat ref_counts.nt         \
          surface_forms.nt      \
          instance_types_en.nt  \
          short_abstracts_en.nt \
          article_categories_en.nt | sort >data-to-be-indexed.nt

5. Get the dataset redirects_en.nt. Redirects are not indexed, but they are
   excluded as targets of lookup.

6. Run Indexer with three arguments:
   1) target Lucene directory (create an empty one if necessary),
      e.g. 'c:\lucene_lookup_index'
   2) redirects dataset,
      e.g. 'c:\redirects_en.nt'
   3) concatenated and sorted data,
      e.g. 'c:\data-to-be-indexed.nt '

   mvn scala:run -Dlauncher=Indexer "-DaddArgs=c:\lucene_lookup_index|c:\redirects_en.nt|c:\data-to-be-indexed.nt"

