package org.dbpedia.lookup.lucene

import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.util.Version
import org.apache.lucene.analysis._
import java.io.{Reader, File}
import standard.{StandardFilter, StandardAnalyzer}
import org.apache.lucene.queryParser.QueryParser
import org.dbpedia.lookup.util.Logging

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 14.01.11
 * Time: 15:10
 * Lucene configuration data.
 */

object LuceneConfig extends Logging {

    // default_index_path is not used any more

    // Overwrite existing directories when indexing (must be true if target directory does not exist)
    val overwriteExisting = true

    // number of data points to read in memory before updating the index
    val commitAfterDataPointsNum = 1500000

    // Lucene Version
    val version = Version.LUCENE_36

    // Analyzer for KeywordSearch
    val analyzer = new StandardAnalyzer(version, StopAnalyzer.ENGLISH_STOP_WORDS_SET)

    // index writer configuration
    val indexWriterConfig = new IndexWriterConfig(version, analyzer)

    //HACK!: Analyzer for PrefixSearch. The result is converted back to a string and indexed/search NOT_ANALYZED!
    object PrefixSearchPseudoAnalyzer {
        private val prefixSearchQueryParser = new QueryParser(version, Fields.SURFACE_FORM_KEYWORD, analyzer)

        def analyze(keyword: String) = {
            prefixSearchQueryParser.parse('"' + QueryParser.escape(keyword) + '"')
                    .toString.replace(Fields.SURFACE_FORM_KEYWORD+":", "")
                             .replaceFirst("^\"", "")
                             .replaceFirst("\"$", "")
                             .toLowerCase
        }
    }

    object Fields {
        val URI = "URI"
        val SURFACE_FORM_KEYWORD = "SURFACE_FORM_KEYWORD"
        val SURFACE_FORM_PREFIX = "SURFACE_FORM_PREFIX"
        val REFCOUNT = "REFCOUNT"

        val DESCRIPTION = "DESCRIPTION"
        val CLASS = "CLASS"
        val CATEGORY = "CATEGORY"
        val TEMPLATE = "TEMPLATE"
        val REDIRECT = "REDIRECT"
    }

}
