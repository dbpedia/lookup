package org.dbpedia.lookup.server

import org.dbpedia.lookup.util.WikiUtil
import org.apache.commons.lang.StringEscapeUtils

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 18.01.11
 * Time: 13:57
 * Class that capture one result from the index.
 */

class Result(uri: String,
             description: String,
             ontologyClasses: List[String],
             categories: Set[String],
             templates: Set[String],
             redirects: Set[String],
             refCount: Int) {

    // get the label of this result URI
    def label = WikiUtil.wikiDecode(uri.replace("http://dbpedia.org/resource/", ""))

    // make a class label from a class URI (de-camel-case)
    def classLabel(classUri: String): String = {
        if(classUri endsWith "owl#Thing") {
            return "owl#Thing"
        }
        val s = WikiUtil.wikiDecode(classUri.replace("http://dbpedia.org/ontology/", ""))
        s.replaceAll("([A-Z])", " $1").trim.toLowerCase
    }

    // make a category label from a category URI
    def categoryLabel(categoryUri: String): String = {
        WikiUtil.wikiDecode(categoryUri.replace("http://dbpedia.org/resource/Category:", ""))
    }


    def toXml = {
        val sb = new StringBuilder

        sb.append("  <Result>\n")
        sb.append("    <Label>"+StringEscapeUtils.escapeXml(label)+"</Label>\n")
        sb.append("    <URI>"+StringEscapeUtils.escapeXml(uri)+"</URI>\n")

        if(description == null) {
            sb.append("    <Description />\n")
        }
        else {
            sb.append("    <Description>"+StringEscapeUtils.escapeXml(description)+"</Description>\n")
        }

        if(ontologyClasses == null || ontologyClasses.isEmpty) {
            sb.append("    <Classes />\n")
        }
        else {
            sb.append("    <Classes>\n")
            for(c <- ontologyClasses) {
                sb.append("      <Class>\n")
                sb.append("        <Label>"+StringEscapeUtils.escapeXml(classLabel(c))+"</Label>\n")
                sb.append("        <URI>"+StringEscapeUtils.escapeXml(c)+"</URI>\n")
                sb.append("      </Class>\n")
            }
            sb.append("    </Classes>\n")
        }

        if(categories == null || categories.isEmpty) {
            sb.append("    <Categories />\n")
        }
        else {
            sb.append("    <Categories>\n")
            for(c <- categories) {
                sb.append("      <Category>\n")
                sb.append("        <Label>"+StringEscapeUtils.escapeXml(categoryLabel(c))+"</Label>\n")
                sb.append("        <URI>"+StringEscapeUtils.escapeXml(c)+"</URI>\n")
                sb.append("      </Category>\n")
            }
            sb.append("    </Categories>\n")
        }

        if(templates == null || templates.isEmpty) {
            sb.append("    <Templates />\n")
        }
        else {
            sb.append("    <Templates>\n")
            templates.foreach(c => sb.append("      <URI>"+c+"</URI>\n"))
            sb.append("    </Templates>\n")
        }

        if(redirects == null || redirects.isEmpty) {
            sb.append("    <Redirects />\n")
        }
        else {
            sb.append("    <Redirects>\n")
            redirects.foreach(r => sb.append("      <URI>"+r+"</URI>\n"))
            sb.append("    </Redirects>\n")
        }

        sb.append("    <Refcount>"+refCount+"</Refcount>\n")
        sb.append("  </Result>")

        sb.toString
    }

}

object Result {

    private val xmlTemplate =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
        "<ArrayOfResult xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "+
        "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://lookup.dbpedia.org/\">\n"+
        "%s"+
        "\n</ArrayOfResult>"

    def getXml(results: Traversable[Result]) = xmlTemplate.format(results.map(_.toXml).mkString("\n"))

}