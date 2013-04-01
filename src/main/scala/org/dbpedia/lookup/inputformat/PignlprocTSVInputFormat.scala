package org.dbpedia.lookup.inputformat

import java.io.InputStream
import org.dbpedia.lookup.lucene.LuceneConfig
import io.Source
import org.dbpedia.extraction.util.WikiUtil

/**
 * Class to itereate over a pignlproc nerd-stats result.
 */

class PignlprocTSVInputFormat(dataSet: InputStream) extends Traversable[(String,String,String)] {

    private val it = Source.fromInputStream(dataSet, "utf-8").getLines()

    override def foreach[U](f: ((String,String,String)) => U) {

        while(it.hasNext) {
            val elements = it.next().split("\t")

            val uri = WikiUtil.wikiEncode(elements(0))
            val sf = elements(1)
            val pUriGivenSf = elements(2).toDouble
            val pSfGivenUri = elements(3).toDouble
            val pSf = elements(4).toDouble
            val wikiPageId = elements(5).toInt
            val uriCount = elements(6).toInt

            f(uri, LuceneConfig.Fields.SURFACE_FORM_KEYWORD, sf)
            f(uri, LuceneConfig.Fields.REFCOUNT, uriCount)
        }

    }

}