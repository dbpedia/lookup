FROM java:8

MAINTAINER  DBpedia Team <dbpedia-developers@lists.sourceforge.net>

RUN apt-get update && apt-get install -y \
    curl

ENV INDEX_URL downloads.dbpedia-spotlight.org/dbpedia_lookup/models
ENV INDEX_FILENAME 2015-10.tar.gz

ENV LOOKUP_JAR dbpedia-lookup-3.1-jar-with-dependencies.jar
ENV LOOKUP_URL downloads.dbpedia-spotlight.org/dbpedia_lookup/

RUN mkdir -p /opt/lookup && \
    cd /opt/lookup && \
    wget "http://$LOOKUP_URL/$LOOKUP_JAR" -O $LOOKUP_JAR  && \
    wget "http://$INDEX_URL/$INDEX_FILENAME" -O $INDEX_FILENAME  && \
    tar xvf $INDEX_FILENAME   && \
    rm  $INDEX_FILENAME

EXPOSE 1111
