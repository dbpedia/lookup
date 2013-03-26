package org.dbpedia.lookup.lucene

import org.apache.lucene.store.FSDirectory
import org.apache.lucene.document.{Field, Document}
import org.apache.lucene.index.{Term, IndexWriter}
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.{FileInputStream, InputStream, InputStreamReader, File}
import java.util.Properties
import org.semanticweb.yars.nx.parser.NxParser
import org.dbpedia.lookup.util.{DBpedia2Lucene, WikiUtil}

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 14.01.11
 * Time: 17:01
 * Indexes the lookup data to a Lucene directory.
 * 
 * Usage : mvn scala:run -Dlauncher=Indexer -DaddArgs=indexing.properties
 * or mvn scala:run -Dlauncher=Indexer "-DaddArgs=indexing.properties|redirects.nt" to input another redirects file than the one given in indexing.properties 
 * 
 * See createIndexFromSpotlightIndex.sh to generate the input files for the indexer from Spotlight index and DBpedia dumps. 
 */

class Indexer(
    val indexDir: File = LuceneConfig.defaultIndex,
	val langCode : String = "en",
	val resourceNamesSpace : String = "http://dbpedia.org/resource/"
) {

    private val indexWriter = new IndexWriter(FSDirectory.open(indexDir), LuceneConfig.analyzer, LuceneConfig.overwriteExisting, LuceneConfig.maxFieldLen)
    System.err.println("Directory "+indexDir+" opened for indexing")

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
                System.err.println(count+" triples read")
            }
            if(count%LuceneConfig.commitAfterNTriples == 0) {
                System.err.println("Commiting")
                indexWriter.commit
            }
        }

        val uriTerm = new Term(LuceneConfig.Fields.URI, currentUri)
        indexWriter.updateDocument(uriTerm, getDocument(uriTerm, fieldCollector))

        System.err.println("Final commit")
        indexWriter.commit
        System.err.println(count+" triples indexed. Done")
    }

    def close() {
        if(LuceneConfig.optimize) {
            System.err.println("Optimizing index...")
            indexWriter.optimize
        }
        indexWriter.close
        System.err.println("Closed index "+indexDir)
    }


    private def getDocument(uriTerm: Term, fields: Map[String,Set[String]]): Document = {
        val doc = new Document()
        doc.add(new Field(LuceneConfig.Fields.URI, uriTerm.text, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO))

        val label = WikiUtil.wikiDecode(uriTerm.text.replace(resourceNamesSpace, ""))
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


object Indexer {

    /**
     * Index data to a directory.
     */
    def main(args: Array[String]) {
        
        // reading params from param file
        val in = new FileInputStream(args(0))
        val properties = new Properties()
        try properties.load(new InputStreamReader(in, "UTF-8"))
        finally in.close()
        val langCode = properties.getProperty("org.dbpedia.spotlight.language_i18n_code","en")
        val resourceNamesSpace = properties.getProperty("org.dbpedia.spotlight.default_namespace","http://dbpedia.org/resource/")
        val indexDir = new File(properties.getProperty("org.dbpedia.lookup.lookup_index_dir_lucene"))
        val data = properties.getProperty("org.dbpedia.lookup.lookup_index_rdf").split(",")
        
        var redirectsFile = new File(properties.getProperty("org.dbpedia.spotlight.data.redirects"))
        // TODO skip the comment lines (not supported by NxParser)
        // for now this is done by a bash command that generates a temporary redirects file
        if(args.length>1){
        	redirectsFile = new File(args(1));
        }

        val redirects = getRedirectUris(redirectsFile)

        val indexer = new Indexer(indexDir, langCode, resourceNamesSpace)

        for(fileName <- data) {
            var in: InputStream = new FileInputStream(fileName)
            if(fileName.endsWith(".bz2")) {
                in = new BZip2CompressorInputStream(in)
            }

            System.err.println("Indexing "+fileName)
            indexer.index(in, redirects)
            System.err.println("Done Indexing "+fileName)
        }
        indexer.close
    }


    private def getRedirectUris(redirectsFile: File): Set[String] = {
        var reds = Set[String]()
        System.err.println("Reading redirects from "+redirectsFile)
        var in: InputStream = new FileInputStream(redirectsFile)
        if(redirectsFile.getPath().endsWith(".bz2")) {
                in = new BZip2CompressorInputStream(in)
        }
        
        val parser = new NxParser(in)
        while (parser.hasNext) {
            val triple = parser.next
            if(triple(1).toString != "http://dbpedia.org/ontology/wikiPageRedirects") {
                throw new Exception("predicate must be http://dbpedia.org/ontology/wikiPageRedirects; got "+triple(1).toString)
            }
            reds = reds + triple(0).toString
        }
        System.err.println("Done")
        reds
    }

}
