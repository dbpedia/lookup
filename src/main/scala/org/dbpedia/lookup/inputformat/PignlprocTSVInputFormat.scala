package org.dbpedia.lookup.inputformat

import java.io.InputStream
import org.dbpedia.lookup.lucene.LuceneConfig
import scala.io.Source
import org.dbpedia.extraction.util.WikiUtil

/**
 * Class to itereate over a pignlproc nerd-stats result.
 */

class PignlprocTSVInputFormat(dataSet: InputStream, pSfGivenUriThreshold: Double, uriField: Int=0, sfField: Int=1, pSfGivenUriField: Int=3, refCountField: Int=6)
    extends InputFormat {

    val DBPEDIA_RESOURCE_NAMESPACE = "http://dbpedia.org/resource/"

    private val it = Source.fromInputStream(dataSet, "utf-8").getLines()

    override def foreach[U](f: ((String,String,String)) => U) {

        while(it.hasNext) {
            val elements = it.next().split("\t")

            val uri = DBPEDIA_RESOURCE_NAMESPACE + WikiUtil.wikiEncode(elements(uriField))
            val sf = elements(sfField)
            //val pUriGivenSf = elements(2)
            val pSfGivenUri = elements(pSfGivenUriField)
            //val pSf = elements(4)
            //val wikiPageId = elements(5)
            val uriCount = elements(refCountField)

            if (pSfGivenUri.toDouble > pSfGivenUriThreshold) {
                f( (uri, LuceneConfig.Fields.SURFACE_FORM_KEYWORD, sf) )
            }
            f( (uri, LuceneConfig.Fields.REFCOUNT, uriCount) )
        }

    }

}
