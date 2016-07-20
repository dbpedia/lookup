package org.dbpedia.lookup.inputformat

import org.semanticweb.yars.nx.parser.NxParser
import java.io.InputStream
import org.dbpedia.lookup.lucene.LuceneConfig

/**
 * Class to itereate over DBpedia NTriples dataset and
 */
class DBpediaNTriplesInputFormat(val dataSet: InputStream, val redirects: scala.collection.Set[String]) extends InputFormat {

  private val it = new NxParser(dataSet)

  val predicate2field = Map(
    "http://lexvo.org/ontology#label" -> LuceneConfig.Fields.SURFACE_FORM_KEYWORD, // no DBpedia dataset, has to be created
    "http://dbpedia.org/property/refCount" -> LuceneConfig.Fields.REFCOUNT, // no DBpedia dataset, has to be created
    "http://dbpedia.org/ontology/abstract" -> LuceneConfig.Fields.DESCRIPTION,
    "http://www.w3.org/2000/01/rdf-schema#comment" -> LuceneConfig.Fields.DESCRIPTION,
    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" -> LuceneConfig.Fields.CLASS,
    "http://purl.org/dc/terms/subject" -> LuceneConfig.Fields.CATEGORY,
    "http://dbpedia.org/property/wikiPageUsesTemplate" -> LuceneConfig.Fields.TEMPLATE, // not really necessary
    "http://dbpedia.org/ontology/wikiPageRedirects" -> LuceneConfig.Fields.REDIRECT // not really necessary
    )

  override def foreach[U](f: ((String, String, String)) => U) {

    while (it.hasNext) {
      val triple = it.next
      val uri = triple(0).toString
      val pred = triple(1).toString
      val obj = triple(2).toString

      predicate2field.get(pred) match {
        case Some(field: String) if (redirects.isEmpty || !redirects.contains(uri)) => {
          if (field == LuceneConfig.Fields.REDIRECT) {
            f((obj, field, uri)) // make it a "hasRedirect" relation
          } else {
            f((uri, field, obj))
          }
        }
        case _ =>
      }
    }

  }

}