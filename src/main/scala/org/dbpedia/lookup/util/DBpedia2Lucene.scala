package org.dbpedia.lookup.util

import org.semanticweb.yars.nx.parser.NxParser
import java.io.InputStream
import org.dbpedia.lookup.lucene.LuceneConfig

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 18.01.11
 * Time: 10:51
 * Class to itereate over DBpedia NTriples dataset and
 */

class DBpedia2Lucene(val dataSet: InputStream, val redirects: Set[String]) extends Traversable[(String,String,String)] {

    private val it = new NxParser(dataSet)

    val predicate2field = Map(
        "http://lexvo.org/ontology#label" -> LuceneConfig.Fields.SURFACE_FORM,   // no DBpedia dataset, has to be created
        "http://lexvo.org/id/label" -> LuceneConfig.Fields.SURFACE_FORM,         // no DBpedia dataset, has to be created    //TODO delete
        "http://dbpedia.org/property/refCount" -> LuceneConfig.Fields.REFCOUNT,  // no DBpedia dataset, has to be created
        "http://dbpedia.org/ontology/abstract" -> LuceneConfig.Fields.DESCRIPTION,
        "http://www.w3.org/2000/01/rdf-schema#comment" -> LuceneConfig.Fields.DESCRIPTION,
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" -> LuceneConfig.Fields.CLASS,
        "http://purl.org/dc/terms/subject" -> LuceneConfig.Fields.CATEGORY,
        "http://dbpedia.org/property/wikiPageUsesTemplate" -> LuceneConfig.Fields.TEMPLATE,  // not really necessary
        "http://dbpedia.org/ontology/wikiPageRedirects" -> LuceneConfig.Fields.REDIRECT      // not really necessary
    )

    override def foreach[U](f: ((String,String,String)) => U) {

        while(it.hasNext) {
            val triple = it.next
            val uri = triple(0).toString
            val pred = triple(1).toString
            val obj = triple(2).toString

            predicate2field.get(pred) match {
                case Some(field: String) if(redirects.isEmpty || !redirects.contains(uri)) => {
                    if(field == LuceneConfig.Fields.REDIRECT) {   // make it a "hasRedirect" relation
                        f( (obj, field, uri) )                    //TODO FIXME this conflicts with the indexing policy of sorting by subject URI (redirects will not be correct in the index)
                    }
                    else {
                        f( (uri, field, obj) )
                    }
                }
                case _ =>
            }
        }

    }

}