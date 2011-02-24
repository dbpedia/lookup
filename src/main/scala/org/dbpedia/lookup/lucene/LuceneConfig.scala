package org.dbpedia.lookup.lucene

import org.apache.lucene.index.IndexWriter
import org.apache.lucene.util.Version
import io.Source
import org.apache.lucene.analysis._
import java.io.{Reader, File}
import standard.{StandardFilter, StandardAnalyzer}

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 14.01.11
 * Time: 15:10
 * Lucene configuration data.
 */

object LuceneConfig {

    val INDEX_CONFIG_FILE = "default_index_path"

    // Default index directory is read from the configuration file
    private val defaultIndexDir = new File(Source.fromFile(INDEX_CONFIG_FILE).getLines.next)
    def defaultIndex: File = {
        System.err.println("INFO: using default index specified in '"+INDEX_CONFIG_FILE+"': "+defaultIndexDir)
        if(!defaultIndexDir.isDirectory) {
            System.err.println("WARNING: "+defaultIndexDir+" is not a valid directory.")
        }
        defaultIndexDir
    }

    // Overwrite existing directories when indexing (must be true if target directory does not exist)
    val overwriteExisting = true

    val commitAfterNTriples = 2000000

    // Optimize index after indexing
    val optimize = true

    // Maximum field length for the fields defined below
    val maxFieldLen = new IndexWriter.MaxFieldLength(2000)

    // Lucene Version
    val version = Version.LUCENE_30


    class DualAnalyzer extends Analyzer {
        private val standardAnalyzer = new StandardAnalyzer(version, StopAnalyzer.ENGLISH_STOP_WORDS_SET)
        private val keywordAnalyzer = new KeywordAnalyzer

        override def tokenStream(fieldName: String, reader: Reader): TokenStream = {
            if(fieldName == Fields.SURFACE_FORM_PREFIX) {
                new LowerCaseFilter( keywordAnalyzer.tokenStream(fieldName, reader) )
            }
            else {
                standardAnalyzer.tokenStream(fieldName, reader)
            }
        }
    }

    //val analyzer = new DualAnalyzer
    val analyzer = new StandardAnalyzer(version, StopAnalyzer.ENGLISH_STOP_WORDS_SET)

    object Fields {
        val URI = "URI"
        val SURFACE_FORM = "SURFACE_FORM"
        val SURFACE_FORM_PREFIX = "SURFACE_FORM_PREFIX"
        val REFCOUNT = "REFCOUNT"

        val DESCRIPTION = "DESCRIPTION"
        val CLASS = "CLASS"
        val CATEGORY = "CATEGORY"
        val TEMPLATE = "TEMPLATE"
        val REDIRECT = "REDIRECT"
    }

}