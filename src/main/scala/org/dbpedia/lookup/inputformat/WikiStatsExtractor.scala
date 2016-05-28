package org.dbpedia.lookup.inputformat

import java.io.InputStream

import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.lookup.lucene.LuceneConfig

import scala.io.Source

class WikiStatsExtractor(dataSet: InputStream, pSfGivenUriThreshold: Double) extends InputFormat {

  private val it = Source.fromInputStream(dataSet, "utf-8").getLines()

  override def foreach[U](f: ((String,String,String)) => U) {

    while(it.hasNext) {
      val elements = it.next().split("\t")

      if (elements.size >= 3) {
        val uri = WikiUtil.wikiEncode(elements(1))
        val sf = elements(0)

        val uriCount = elements(2)

        f((uri, LuceneConfig.Fields.SURFACE_FORM_KEYWORD, sf))
        f((uri, LuceneConfig.Fields.REFCOUNT, uriCount))
      } 
    }
  }
}

