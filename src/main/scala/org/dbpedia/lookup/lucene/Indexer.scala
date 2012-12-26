package org.dbpedia.lookup.lucene

import org.apache.lucene.store.FSDirectory
import org.apache.lucene.document.{Field, Document}
import org.apache.lucene.index.{Term, IndexWriter}
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.{FileInputStream, InputStream, File}
import org.semanticweb.yars.nx.parser.NxParser
import org.dbpedia.lookup.util.{DBpedia2Lucene, WikiUtil, Logging}

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 14.01.11
 * Time: 17:01
 * Indexes the lookup data to a Lucene directory.
 */

class Indexer(val indexDir: File = LuceneConfig.defaultIndex) extends Logging {

    private val indexWriter = new IndexWriter(FSDirectory.open(indexDir), LuceneConfig.analyzer, LuceneConfig.overwriteExisting, LuceneConfig.maxFieldLen)
    logger.info("Directory "+indexDir+" opened for indexing")

    /**
     * Index a data file for the lookup service.
     */
    def index(dataSetStream: InputStream, redirects: Set[String]) {
        //TODO CAUTION: this assumes sorted input!

        //cat dataset1.nt dataset2.nt dataset3.nt | sort >indexdata.nt

        var count = 0
        var currentUri = ""
        var fieldCollector = Map[String,Set[String]]()

        for((uri, field, value) <- new DBpedia2Lucene(dataSetStream, redirects)) {

            if(currentUri != "" && currentUri != uri) {
                if(fieldCollector.nonEmpty) {
                    val uriTerm = new Term(LuceneConfig.Fields.URI, currentUri)
                    indexWriter.updateDocument(uriTerm, getDocument(uriTerm, fieldCollector))
                }
                fieldCollector = Map[String,Set[String]]()
            }

            fieldCollector = fieldCollector.updated(field, fieldCollector.get(field).getOrElse(Set()) + value)
            currentUri = uri

            count += 1
            if(count%250000 == 0) {
                logger.info(count+" triples read")
            }
            if(count%LuceneConfig.commitAfterNTriples == 0) {
                logger.info("Commiting")
                indexWriter.commit
            }
        }

        val uriTerm = new Term(LuceneConfig.Fields.URI, currentUri)
        indexWriter.updateDocument(uriTerm, getDocument(uriTerm, fieldCollector))

        logger.info("Final commit")
        indexWriter.commit
        logger.info(count+" triples indexed. Done")
    }

    def close() {
        if(LuceneConfig.optimize) {
            logger.info("Optimizing index...")
            indexWriter.optimize
        }
        indexWriter.close
        logger.info("Closed index "+indexDir)
    }


    private def getDocument(uriTerm: Term, fields: Map[String,Set[String]]): Document = {
        val doc = new Document()
        doc.add(new Field(LuceneConfig.Fields.URI, uriTerm.text, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO))

        val label = WikiUtil.wikiDecode(uriTerm.text.replace("http://dbpedia.org/resource/", ""))
        doc.add(new Field(LuceneConfig.Fields.SURFACE_FORM_KEYWORD, label, Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.NO))

        val prefixTerm = LuceneConfig.PrefixSearchPseudoAnalyzer.analyze(label)
        doc.add(new Field(LuceneConfig.Fields.SURFACE_FORM_PREFIX, prefixTerm, Field.Store.NO, Field.Index.NOT_ANALYZED, Field.TermVector.NO))

        for((field, valueSet) <- fields) {
            for(value <- valueSet) {
                if(field == LuceneConfig.Fields.SURFACE_FORM_KEYWORD) {
                    doc.add(new Field(LuceneConfig.Fields.SURFACE_FORM_KEYWORD, value, Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.NO))

                    val prefixTerm = LuceneConfig.PrefixSearchPseudoAnalyzer.analyze(value)
                    doc.add(new Field(LuceneConfig.Fields.SURFACE_FORM_PREFIX, prefixTerm, Field.Store.NO, Field.Index.NOT_ANALYZED, Field.TermVector.NO))
                }
                else {
                    doc.add(new Field(field, value, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO))
                }
            }
        }

        doc
    }

}


object Indexer extends Logging {

    /**
     * Index data to a directory.
     */
    def main(args: Array[String]) {
        val indexDir = new File(args.head)
        val redirectsFile = new File(args.tail.head)
        val data = args.tail.tail

        val redirects = getRedirectUris(redirectsFile)

        val indexer = new Indexer(indexDir)

        for(fileName <- data) {
            var in: InputStream = new FileInputStream(fileName)
            if(fileName.endsWith(".bz2")) {
                in = new BZip2CompressorInputStream(in)
            }

            logger.info("Indexing "+fileName)
            indexer.index(in, redirects)
            logger.info("Done Indexing "+fileName)
        }
        indexer.close
    }


    private def getRedirectUris(redirectsFile: File): Set[String] = {
        var reds = Set[String]()
        logger.info("Reading redirects from "+redirectsFile)
        val parser = new NxParser(new FileInputStream(redirectsFile))
        while (parser.hasNext) {
            val triple = parser.next
            if(triple(1).toString != "http://dbpedia.org/ontology/wikiPageRedirects") {
                throw new Exception("predicate must be http://dbpedia.org/ontology/wikiPageRedirects; got "+triple(1).toString)
            }
            reds = reds + triple(0).toString
        }
        logger.info("Done")
        reds
    }

}
