#!/bin/bash

CONF_FILE=./indexing.properties

# reads params from CONF_FILE
function getParam {
 echo $(grep "$1[ ]*=" $CONF_FILE | sed "s/.*=[ ]*\(.*\)/\1/");
}

# generates command to parse the files (bzcat for .bz2 files, gunzip -c for .gz files or cat for other extensions)
# removes comments (lines that begin with #), and replaces tabs with single spaces
function getCatCmd {
  if [[ $1 == *.bz2 ]]
    then  cmd="bzcat $1"
  elif [[ $1 == *.gz ]]
    then  cmd="gunzip -c $1"
  else cmd="cat $1"
  fi
  echo "$cmd | grep -v '^#' | sed -r 's/[ \t]+/ /g'"
}

# generate input file for Lookup Indexer from Spotlight surface form
mvn scala:run -Dlauncher=CreateLookupIndex -DaddArgs=$CONF_FILE

# extract needed output for index indexer
parsedSurfaceForm=$(getParam "org.dbpedia.lookup.data.surfaceFormParsingOutput");
echo "parsedSurfaceForm : $parsedSurfaceForm"


# adding labels to the surface forms
labels_dump=$(getParam "org.dbpedia.spotlight.data.labels");
echo "label dump : $labels_dump"
cmd=$(getCatCmd $labels_dump)
cmd="$cmd | sed 's/<http:\/\/www.w3.org\/2000\/01\/rdf-schema#label>/<http:\/\/lexvo.org\/ontology#label>/'"
cmd="$cmd >> $parsedSurfaceForm"
eval $cmd

# adding the instances types 
instance_types_dump=$(getParam "org.dbpedia.spotlight.data.instanceTypes");
echo "instance_types_dump : $instance_types_dump"
cmd=$(getCatCmd $instance_types_dump)
cmd="$cmd | grep 'http://dbpedia.org/ontology/'"
cmd="$cmd >> $parsedSurfaceForm"
eval $cmd

# adding the short abstracts
#short_abstracts_dump=$(getParam "org.dbpedia.spotlight.data.shortAbstracts");
#echo "short_abstracts_dump : $short_abstracts_dump"
#cmd=$(getCatCmd $short_abstracts_dump)
#cmd="$cmd >> $parsedSurfaceForm"
#eval $cmd

# adding the categories
categories_dump=$(getParam "org.dbpedia.spotlight.data.article_Categories");
echo "categories_dump : $categories_dump"
cmd=$(getCatCmd $categories_dump)
cmd="$cmd >> $parsedSurfaceForm"
eval $cmd


# sorting and cleaning
echo "sorting and removing duplicates"
rdf_index_file=$(getParam "org.dbpedia.lookup.lookup_index_rdf");
sort $parsedSurfaceForm | uniq > $rdf_index_file

# getting a clean version of redirects (no comments)
redirects_dump=$(getParam "org.dbpedia.spotlight.data.redirects");
cmd=$(getCatCmd $redirects_dump)
cmd="$cmd > redirects-temp.nt"
eval $cmd

# running Lookup Indexer
echo "running Lookup indexer"
index_dir=$(getParam "org.dbpedia.lookup.lookup_index_dir_lucene");
#mvn scala:run -Dlauncher=Indexer "-DaddArgs=$index_dir|redirects-temp.nt|$rdf_index_file"
mvn scala:run -Dlauncher=Indexer "-DaddArgs=$CONF_FILE|redirects-temp.nt"
#echo "-DaddArgs=$index_dir|$redirects_dump|$rdf_index_file"

#rm redirects-temp.nt
