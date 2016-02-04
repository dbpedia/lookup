#!/bin/bash

readonly DBPEDIA_VERSION=3.9
readonly DBPEDIA_DOWNLOADS="http://downloads.dbpedia.org"/$DBPEDIA_VERSION
readonly LANG_i18n=en
readonly DBPEDIA_DATA=~/lookup/dbpedia_data/$DBPEDIA_VERSION
readonly DBPEDIA_INDEX=~/lookup/dbpedia-lookup-index/$LANG_i18n/$DBPEDIA_VERSION
readonly ALL_FILES=(redirects short_abstracts instance_types article_categories)

#+------------------------------------------------------------------------------------------------------------------------------+
#| Functions                                                                                                                    |
#+------------------------------------------------------------------------------------------------------------------------------+

# Error_exit function by William Shotts. http://stackoverflow.com/questions/64786/error-handling-in-bash
function error_exit
{
    echo -e "${PROGNAME}: ${1:-"Unknown Error"}" 1>&2
    exit 1
}

# The function used to create all the directories needed
function create_dir()
{
    if [ -e $1 ]; then
        echo -e $1" already exists. Skipping creating this directory!"
    else
        mkdir -p $1
    fi
}

# A helper function to download files from a given path. The first parameter is the path from where to download the file
# without the file name, the second states the file name, and the third is where to save that file
function download_file()
{
    # Only downloads if there is no current file or there is a newer version
    echo "$#"
    case "$#" in
        "3")
            wget -q --spider $1/$2
            if [ $? -eq 0 ] ; then
                wget -N $1/$2 --directory-prefix=$3
            else
                # The file can't be found. We can extract a substring with the file name and show it to the user
                error_exit "ERROR: The file '"$2"' cannot be found for download.\n"
            fi
            ;;
        "4")
            wget -q --spider $1 $2/$3
            if [ $? -eq 0 ] ; then
                wget -N $1 $2/$3 --directory-prefix=$4
            else
                # The file can't be found. We can extract a substring with the file name and show it to the user
                error_exit "ERROR: The file '"$3"' cannot be found for download.\n"
            fi
            ;;
        *)
            error_exit "ERROR: Incorrect number of parameters!";
    esac
    echo -e "done!\n"
}

#-----------------------------------------------------------------------------------------------------------------------------+
create_dir $DBPEDIA_DATA
create_dir $DBPEDIA_INDEX

for i in ${ALL_FILES[@]}
do
 download_file $DBPEDIA_DOWNLOADS/$LANG_i18n ${i}_$LANG_i18n.nt.bz2 $DBPEDIA_DATA/$LANG_i18n    
done

for i in ${ALL_FILES[@]}
do
  bunzip2 -fk $DBPEDIA_DATA/$LANG_i18n/${i}_$LANG_i18n.nt.bz2 >  $DBPEDIA_DATA/$LANG_i18n/${i}_$LANG_i18n.nt
done

cat $DBPEDIA_DATA/$LANG_i18n/short_abstracts_$LANG_i18n.nt $DBPEDIA_DATA/$LANG_i18n/instance_types_$LANG_i18n.nt $DBPEDIA_DATA/$LANG_i18n/article_categories_$LANG_i18n.nt > $DBPEDIA_DATA/$LANG_i18n/all_dbpedia_data.nt

git clone https://github.com/dbpedia/lookup.git
cd lookup
mvn clean install

./run Indexer $DBPEDIA_INDEX $DBPEDIA_DATA/$LANG_i18n/redirects_$LANG_i18n.nt $DBPEDIA_DATA/$LANG_i18n/all_dbpedia_data.nt
