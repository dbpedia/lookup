package org.dbpedia.lookup.lucene

import org.apache.lucene.store.FSDirectory
import org.apache.lucene.document.{Field, Document}
import org.apache.lucene.index.{IndexReader, Term, IndexWriter}
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.{FileInputStream, InputStream, File}
import org.semanticweb.yars.nx.parser.NxParser
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.lookup.inputformat.{WikiStatsExtractor, InputFormat, DBpediaNTriplesInputFormat, PignlprocTSVInputFormat}
import org.apache.lucene.search.{IndexSearcher, TermQuery}
import org.dbpedia.lookup.util.Logging

/**
 * Indexes the lookup data to a Lucene directory.
 */
class Indexer(val indexDir: File) extends Logging {

    private val indexWriter = new IndexWriter(FSDirectory.open(indexDir), LuceneConfig.indexWriterConfig)
    indexWriter.commit()
    private val indexSearcher = new IndexSearcher(IndexReader.open(FSDirectory.open(indexDir)))
    logger.info("Directory "+indexDir+" opened for indexing")

    /**
     * Index a data file for the lookup service.
     */
    def index(dataTraversable: InputFormat) {
        var count = 0
        val collector = scala.collection.mutable.HashMap[String, scala.collection.mutable.HashMap[String, scala.collection.mutable.HashSet[String]]]()

        dataTraversable.foreach{ case (uri:String, field:String, value:String) => {
            val fields = collector.getOrElse(uri, scala.collection.mutable.HashMap[String, scala.collection.mutable.HashSet[String]]())
            val values: scala.collection.mutable.HashSet[String] = fields.getOrElse(field, scala.collection.mutable.HashSet[String]())
            values.add(value)
            fields.put(field, values)
            collector.put(uri, fields)

            count += 1
            if(count%100000 == 0) {
                logger.info(count+" data points read")
            }
            if(count%LuceneConfig.commitAfterDataPointsNum == 0) {
                updateIndex(collector)
                collector.clear()
            }
        }}
        updateIndex(collector)
        logger.info(count+" data points indexed. Done")

        //TODO remove?
        logger.info("Optimizing")
        indexWriter.optimize()
        logger.info("Done optimizing")
    }


    private def updateIndex(collector:scala.collection.mutable.HashMap[String,scala.collection.mutable.HashMap[String, scala.collection.mutable.HashSet[String]]]) {
        logger.info("Updating")
        collector.foreach(t => {
            val (uri, fields) = t
            updateDataForUri(uri, fields)
        })
        logger.info("Committing")
        indexWriter.commit()
}

    private def updateDataForUri(currentUri: String, fieldCollector:scala.collection.mutable.HashMap[String, scala.collection.mutable.HashSet[String]]) {

        val uriTerm = new Term(LuceneConfig.Fields.URI, currentUri)
        val hits = indexSearcher.search(new TermQuery(uriTerm), 2)

        val doc =
            if (hits.scoreDocs.length == 1) {
                indexSearcher.doc(hits.scoreDocs(0).doc)
            } else if (hits.scoreDocs.length == 0) {
                val d = new Document
                updateField(d, new Field(LuceneConfig.Fields.URI, uriTerm.text, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO))

                val label = WikiUtil.wikiDecode(uriTerm.text.replace("http://dbpedia.org/resource/", ""))
                updateField(d, new Field(LuceneConfig.Fields.SURFACE_FORM_KEYWORD, label, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.NO))

                val prefixTerm = LuceneConfig.PrefixSearchPseudoAnalyzer.analyze(label)
                updateField(d, new Field(LuceneConfig.Fields.SURFACE_FORM_PREFIX, prefixTerm, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO))

                d
            } else { //  if (hits.scoreDocs.length > 1) {
                throw new IllegalStateException("Given Term matches more than 1 document in the index.")
            }

        indexWriter.updateDocument(uriTerm, getUpdatedDocument(doc, uriTerm, fieldCollector))
    }

    def close() {
        indexWriter.close()
        logger.info("Closed index "+indexDir)
    }


    private def getUpdatedDocument(doc: Document, uriTerm: Term, fields: scala.collection.Map[String, scala.collection.Set[String]]): Document = {
        for((field, valueSet) <- fields) {
            val addedPrefixTerms = new scala.collection.mutable.HashSet[String]()
            for(value <- valueSet) {
                if(field == LuceneConfig.Fields.SURFACE_FORM_KEYWORD) {
                    updateField(doc, new Field(LuceneConfig.Fields.SURFACE_FORM_KEYWORD, value, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.NO))

                    val prefixTerm = LuceneConfig.PrefixSearchPseudoAnalyzer.analyze(value)
                    if (!addedPrefixTerms.contains(prefixTerm)) {
                        updateField(doc, new Field(LuceneConfig.Fields.SURFACE_FORM_PREFIX, prefixTerm, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO))
                        addedPrefixTerms.add(prefixTerm)
                    }
                }
                else {
                    updateField(doc, new Field(field, value, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO))
                }
            }
        }

        doc
    }

    private def updateField(doc: Document, field: Field) {
        doc.add(field)
    }

}


object Indexer extends Logging {

    private val pSfGivenUriThreshold = 0.001

    /**
     * Index data to a directory.
     */
    def main(args: Array[String]) {
        val indexDir = new File(args(0))
        val redirectsFile = new File(args(1))
        val data = args.drop(2)

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
            new DBpediaNTriplesInputFormat(inputStream, getRedirectUris(redirectsFile))
        }
        else if (fileName.contains(".tsv")) {
            logger.debug("using PignlprocTSVInputFormat")
            val refCountField = if (fileName.contains("_alx")) 7 else 6
            new PignlprocTSVInputFormat(inputStream, pSfGivenUriThreshold, refCountField=refCountField)
        } else if (fileName.contains("pairCounts")) {
            new WikiStatsExtractor(inputStream, pSfGivenUriThreshold)
        }
        else {
            throw new IllegalArgumentException("only know how to handle file types .nt, .nq and .tsv")
        }
    }

    private def getRedirectUris(redirectsFile: File): scala.collection.Set[String] = {
        val reds = new scala.collection.mutable.HashSet[String]()
        logger.info("Reading redirects from "+redirectsFile)
        val parser = new NxParser(new FileInputStream(redirectsFile))
        while (parser.hasNext) {
            val triple = parser.next
            if(triple(1).toString != "http://dbpedia.org/ontology/wikiPageRedirects") {
                throw new Exception("predicate must be http://dbpedia.org/ontology/wikiPageRedirects; got "+triple(1).toString)
            }
            reds.add(triple(0).toString)
        }
        logger.info("Done reading redirects")
        reds
    }

}
