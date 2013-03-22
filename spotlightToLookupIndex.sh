#!/bin/bash

export CONF_FILE=./indexing.properties.fr


# generate input file for Lookup Indexer from Spotlight surface form
mvn scala:run -Dlauncher=CreateLookupIndex -DaddArgs=$CONF_FILE

# extract needed output for index indexer
lookup_index_input=$(grep "org.dbpedia.spotlight.data.lookupInputFile[ ]*=" $CONF_FILE | sed "s/.*=[ ]*\(.*\)/\1/");
echo "lookup_index_input : $lookup_index_input"


# adding labels to the surface forms
labels_dump=$(grep "org.dbpedia.spotlight.data.labels[ ]*=" $CONF_FILE | sed "s/.*=[ ]*\(.*\)/\1/");
echo "label dump : $labels_dump"
bzcat $labels_dump | grep -v "^#" | sed "s/<http:\/\/www.w3.org\/2000\/01\/rdf-schema#label>/<http:\/\/lexvo.org\/ontology#label>/" |  sed -r "s/[ \t]+/ /g" >> $lookup_index_input

# adding the instances types 
instance_types_dump=$(grep "org.dbpedia.spotlight.data.instanceTypes[ ]*=" $CONF_FILE | sed "s/.*=[ ]*\(.*\)/\1/");
echo "instance_types_dump : $instance_types_dump"
bzcat $instance_types_dump | grep -v "^#" | grep "http://dbpedia.org/ontology/" |  sed -r "s/[ \t]+/ /g" >> $lookup_index_input

# adding the short abstracts
short_abstracts_dump=$(grep "org.dbpedia.spotlight.data.shortAbstracts[ ]*=" $CONF_FILE | sed "s/.*=[ ]*\(.*\)/\1/");
echo "short_abstracts_dump : $short_abstracts_dump"
bzcat $short_abstracts_dump | grep -v "^#" |  sed -r "s/[ \t]+/ /g" >> $lookup_index_input

# adding the categories
categories_dump=$(grep "org.dbpedia.spotlight.data.article_Categories[ ]*=" $CONF_FILE | sed "s/.*=[ ]*\(.*\)/\1/");
echo "categories_dump : $categories_dump"
bzcat $categories_dump | grep -v "^#" |  sed -r "s/[ \t]+/ /g" >> $lookup_index_input


# sorting and cleaning
rdf_index_file=$(grep "org.dbpedia.lookup.lookup_index_rdf[ ]*=" $CONF_FILE | sed "s/.*=[ ]*\(.*\)/\1/");
sort $lookup_index_input | uniq > $rdf_index_file


# running Lookup Indexer
index_dir=$(grep "org.dbpedia.lookup.lookup_index_dir_lucene[ ]*=" $CONF_FILE | sed "s/.*=[ ]*\(.*\)/\1/");
redirects_dump=$(grep "org.dbpedia.spotlight.data.redirects[ ]*=" $CONF_FILE | sed "s/.*=[ ]*\(.*\)/\1/");
mvn scala:run -Dlauncher=Indexer "-DaddArgs=$index_dir|$redirects_dump|$rdf_index_file"
#echo "-DaddArgs=$index_dir|$redirects_dump|$rdf_index_file"
