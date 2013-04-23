package org.dbpedia.lookup.lucene

import java.io.File
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.search._
import org.apache.lucene.index.{Term, IndexReader}
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.lookup.entities._
import org.apache.lucene.queryParser.QueryParser

/**
 * Created by IntelliJ IDEA.
 * User: Max Jakob
 * Date: 14.01.11
 * Time: 14:43
 * Class to query the Lucene index for the best URI given a surface form.
 */

class Searcher(val indexDir: File) {

    private val indexReader = IndexReader.open(FSDirectory.open(indexDir))
    private val indexSearcher = new IndexSearcher(indexReader)
    private val sort = new Sort(new SortField(LuceneConfig.Fields.REFCOUNT, SortField.INT, true))
    private val queryParser = new QueryParser(LuceneConfig.version, LuceneConfig.Fields.SURFACE_FORM_KEYWORD, LuceneConfig.analyzer)


    def keywordSearch(keyword: String, ontologyClass: String="", maxResults: Int=5): List[Result] = {
        if(keyword == null || keyword.isEmpty) {
            return List.empty
        }
        val query = getQuery(keyword, ontologyClass, prefixQuery = false)
        search(query, maxResults)
    }

    def prefixSearch(keyword: String, ontologyClass: String="", maxResults: Int=5): List[Result] = {
        if(keyword == null || keyword.isEmpty) {
            return List.empty
        }
        val query = getQuery(keyword, ontologyClass, prefixQuery = true)
        search(query, maxResults)
    }

    def close() {
        indexSearcher.close()
        indexReader.close()
    }


    private def search(query: Query, maxResults: Int): List[Result] = {
        indexSearcher.search(query, null, maxResults, sort).scoreDocs.toList.map(getResult)
    }

    private def getQuery(keyword: String, ontologyClass: String, prefixQuery: Boolean = false): Query = {
        val bq = new BooleanQuery

        val decodedKeyword = WikiUtil.wikiDecode(keyword)

        if(prefixQuery) {
            val pseudoAnalyzedKeyword = LuceneConfig.PrefixSearchPseudoAnalyzer.analyze(decodedKeyword)
            val prefixQuery = new PrefixQuery(new Term(LuceneConfig.Fields.SURFACE_FORM_PREFIX, pseudoAnalyzedKeyword))
            bq.add(prefixQuery, BooleanClause.Occur.MUST)
        }
        else {
            val escapedKeyword = QueryParser.escape(decodedKeyword)
            val phraseQuery = synchronized { // query parser is not thread safe!
              queryParser.parse('"' + escapedKeyword + '"')  //quotes keep word order
            }
            bq.add(phraseQuery, BooleanClause.Occur.MUST)
        }

        getOntologyClassQuery(ontologyClass) match {
            case Some(q: Query) => bq.add(q, BooleanClause.Occur.MUST)
            case _ =>
        }

        bq
    }

    private def getOntologyClassQuery(ontologyClass: String): Option[Query] = {
        if(ontologyClass == null || ontologyClass.trim == "") {
            None
        }
        else if(ontologyClass.toLowerCase == "thing" || ontologyClass.toLowerCase.endsWith("#thing")) {
            None
        }
        else {
            val ontologyPrefix = "http://dbpedia.org/ontology/"
            //is full class URI
            if(ontologyClass startsWith ontologyPrefix) {
                Some(new TermQuery(new Term(LuceneConfig.Fields.CLASS, ontologyClass.trim)))
            }
            //abbreviated namespace prefix
            else if(ontologyClass.startsWith("dbpedia:") || ontologyClass.startsWith("dbpedia-owl:")) {
                val c = ontologyClass.trim.replace("dbpedia:", "").replace("dbpedia-owl:", "")
                Some(new TermQuery(new Term(LuceneConfig.Fields.CLASS, ontologyPrefix+c)))
            }
            //label given: make camel case and attach namespace
            else {
                val camel = ontologyClass.trim.split(" ").map(_.capitalize).mkString("")
                Some(new TermQuery(new Term(LuceneConfig.Fields.CLASS, ontologyPrefix+camel)))
            }
        }
    }

    private def getResult(scoreDoc: ScoreDoc): Result = {
        val doc = indexReader.document(scoreDoc.doc)

        val uri: String = doc.get(LuceneConfig.Fields.URI)
        val description: String = doc.get(LuceneConfig.Fields.DESCRIPTION)
        val ontologyClasses: Set[OntologyClass] = doc.getValues(LuceneConfig.Fields.CLASS) match {
            case null => Set.empty
            case classes => classes.map(uri => new OntologyClass(uri)).toSet
        }
        val categories: Set[Category] = doc.getValues(LuceneConfig.Fields.CATEGORY) match {
            case null => Set.empty
            case cats => cats.map(uri => new Category(uri)).toSet
        }
        val templates: Set[Template] = doc.getValues(LuceneConfig.Fields.TEMPLATE) match {
            case null => Set.empty
            case temps => temps.map(uri => new Template(uri)).toSet
        }
        val redirects: Set[Redirect] = doc.getValues(LuceneConfig.Fields.REDIRECT) match {
            case null => Set.empty
            case reds => reds.map(uri => new Redirect(uri)).toSet
        }
        val refCount: Int = doc.get(LuceneConfig.Fields.REFCOUNT) match {
            case null => 0
            case count: String => count.toInt
        }

        new Result(uri, description, ontologyClasses, categories, templates, redirects, refCount)
    }

}
