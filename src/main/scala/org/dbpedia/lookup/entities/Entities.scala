package org.dbpedia.lookup.entities

import org.dbpedia.extraction.util.WikiUtil._

trait Uri   { val uri   : String }
trait Label { val label : String }

case class Redirect(uri: String) extends Uri

case class Template(uri: String) extends Uri

case class Category(uri: String) extends Uri with Label {
    val label: String = wikiDecode(uri.replace("http://dbpedia.org/resource/Category:", ""))
}

case class OntologyClass(uri: String) extends Uri with Label {

    val label: String = {
        if (uri endsWith "owl#Thing") {
            "owl#Thing"
        } else {
            val s = wikiDecode(uri.replace("http://dbpedia.org/ontology/", ""))
                s.replaceAll("([A-Z])", " $1").trim.toLowerCase
        }
        }
    }

case class Result(
    uri: String,
    description: String,
    classes: Set[OntologyClass],
    categories: Set[Category],
    templates: Set[Template],
    redirects: Set[Redirect],
    refCount: Int
) extends Uri with Label {
    val label: String = wikiDecode(uri.replace("http://dbpedia.org/resource/", ""))
}
