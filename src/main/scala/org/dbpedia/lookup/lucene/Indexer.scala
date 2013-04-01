package org.dbpedia.lookup.lucene

import org.apache.lucene.store.FSDirectory
import org.apache.lucene.document.{Field, Document}
import org.apache.lucene.index.{Term, IndexWriter}
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.{FileInputStream, InputStream, File}
import org.semanticweb.yars.nx.parser.NxParser
import org.dbpedia.extraction.util.WikiUtil
import org.slf4j.LoggerFactory
import org.dbpedia.lookup.inputformat.{DBpediaNTriplesInputFormat, PignlprocTSVInputFormat}
import org.apache.lucene.search.{IndexSearcher, TermQuery}

/**
 * Indexes the lookup data to a Lucene directory.
 */

class Indexer(val indexDir: File = LuceneConfig.defaultIndex) extends Logging {

    private val indexWriter = new IndexWriter(FSDirectory.open(indexDir), LuceneConfig.analyzer, LuceneConfig.overwriteExisting, LuceneConfig.maxFieldLen)
    private val indexSearcher = new IndexSearcher(FSDirectory.open(indexDir))
    logger.info("Directory "+indexDir+" opened for indexing")

    /**
     * Index a data file for the lookup service.
     */
    def index(dataTraversable: Traversable[(String, String, String)]) {
        var count = 0
        var currentUri = ""
        var fieldCollector = Map[String,Set[String]]()

        for((uri, field, value) <- dataTraversable) {
            if(currentUri != "" && currentUri != uri) {
                if(fieldCollector.nonEmpty) {
                    updateIndex(currentUri, fieldCollector)
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
                indexWriter.commit()
            }
        }

        updateIndex(currentUri, fieldCollector)

        logger.info("Final commit")
        indexWriter.commit()
        logger.info(count+" triples indexed. Done")
    }


    private def updateIndex(currentUri: String, fieldCollector: Map[String, Set[String]]) {
        val uriTerm = new Term(LuceneConfig.Fields.URI, currentUri)
        val hits = indexSearcher.search(new TermQuery(uriTerm), 2)

        if (hits.scoreDocs.length == 0) {
            throw new IllegalArgumentException("No matches in the index for the given Term.")
        } else if (hits.scoreDocs.length > 1) {
            throw new IllegalArgumentException("Given Term matches more than 1 document in the index.")
        } else {
            // retrieve the old document
            val doc = indexSearcher.doc(hits.scoreDocs(0).doc)
            indexWriter.updateDocument(uriTerm, getUpdatedDocument(doc, uriTerm, fieldCollector))
        }
    }

    def close() {
        if(LuceneConfig.optimize) {
            logger.info("Optimizing index...")
            indexWriter.optimize()
        }
        indexWriter.close()
        logger.info("Closed index "+indexDir)
    }


    private def getUpdatedDocument(doc: Document, uriTerm: Term, fields: Map[String,Set[String]]): Document = {
        //val doc = new Document()
        updateField(doc, new Field(LuceneConfig.Fields.URI, uriTerm.text, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO))

        val label = WikiUtil.wikiDecode(uriTerm.text.replace("http://dbpedia.org/resource/", ""))
        updateField(doc, new Field(LuceneConfig.Fields.SURFACE_FORM_KEYWORD, label, Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.NO))

        val prefixTerm = LuceneConfig.PrefixSearchPseudoAnalyzer.analyze(label)
        updateField(doc, new Field(LuceneConfig.Fields.SURFACE_FORM_PREFIX, prefixTerm, Field.Store.NO, Field.Index.NOT_ANALYZED, Field.TermVector.NO))

        for((field, valueSet) <- fields) {
            for(value <- valueSet) {
                if(field == LuceneConfig.Fields.SURFACE_FORM_KEYWORD) {
                    updateField(doc, new Field(LuceneConfig.Fields.SURFACE_FORM_KEYWORD, value, Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.NO))

                    val prefixTerm = LuceneConfig.PrefixSearchPseudoAnalyzer.analyze(value)
                    updateField(doc, new Field(LuceneConfig.Fields.SURFACE_FORM_PREFIX, prefixTerm, Field.Store.NO, Field.Index.NOT_ANALYZED, Field.TermVector.NO))
                }
                else {
                    updateField(doc, new Field(field, value, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO))
                }
            }
        }

        doc
    }

    private def updateField(doc: Document, field: Field) {
        if (doc.get(field.name() != null)) {
            doc.removeField(field.name())
        }
        updateField(doc, field)
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

        val indexer = new Indexer(indexDir)

        for(fileName <- data) {
            var in: InputStream = new FileInputStream(fileName)
            if (fileName.endsWith(".bz2")) {
                in = new BZip2CompressorInputStream(in)
            }

            logger.info("Indexing "+fileName)
            indexer.index(getDataInput(fileName, in, redirectsFile))
            logger.info("Done Indexing "+fileName)
        }
        indexer.close()
    }

    private def getDataInput(fileName: String, inputStream: InputStream, redirectsFile: File) = {
        if (fileName.contains(".nt") || fileName.contains(".nq")) {
            logger.debug("using DBpediaNTriplesInputFormat")
            new DBpediaNTriplesInputFormat(in, getRedirectUris(redirectsFile))
        }
        else if (fileName.contains(".tsv")) {
            logger.debug("using PignlprocTSVInputFormat")
            new PignlprocTSVInputFormat(inputStream)
        }
        else {
            throw new IllegalArgumentException("only know how to handle file types .nt, .nq and .tsv")
        }
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
