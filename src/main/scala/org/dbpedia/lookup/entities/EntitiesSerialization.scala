package org.dbpedia.lookup.entities

import scala.xml._
import net.liftweb.json._

trait ResultSerializer {
  def prettyPrint(results: Traversable[Result]): String
}

class ResultJsonSerializer extends ResultSerializer {

  def prettyPrint(results: Traversable[Result]): String = {
    import net.liftweb.json.JsonDSL._
    val json = ("results" -> results.map { result =>
      ("uri" -> result.uri) ~
        ("label" -> result.label) ~
        ("description" -> result.description) ~
        ("refCount" -> result.refCount) ~
        ("classes" -> result.classes.map(c => ("uri" -> c.uri) ~ ("label" -> c.label))) ~
        ("categories" -> result.categories.map(c => ("uri" -> c.uri) ~ ("label" -> c.label))) ~
        ("templates" -> result.templates.map(c => ("uri" -> c.uri))) ~
        ("redirects" -> result.redirects.map(c => ("uri" -> c.uri)))
    })
    pretty(render(json))
  }
}
class ResultJsonLDSerializer extends ResultSerializer {

  def prettyPrint(results: Traversable[Result]): String = {
    import net.liftweb.json.JsonDSL._
    var jsonld = ("@context" ->
      ("@vocab" -> "http://dbpedia.org/property/") ~
      ("description" -> "http://dbpedia.org/property/description") ~
      ("refCount" -> "http://dbpedia.org/property/categories") ~
      ("templates" -> "http://dbpedia.org/property/templates") ~
      ("redirects" -> "http://dbpedia.org/ontology/wikiPageRedirects")) ~
      ("results" -> results.map { result =>
        ("uri" -> result.uri) ~
          ("label" -> result.label) ~
          ("description" -> result.description) ~
          ("refCount" -> result.refCount) ~
          ("classes" -> result.classes.map(c => ("uri" -> c.uri) ~ ("label" -> c.label))) ~
          ("categories" -> result.categories.map(c => ("uri" -> c.uri) ~ ("label" -> c.label))) ~
          ("templates" -> result.templates.map(c => ("uri" -> c.uri))) ~
          ("redirects" -> result.redirects.map(c => ("uri" -> c.uri)))
      })

    pretty(render(jsonld))
  }

}

class ResultXmlSerializer extends ResultSerializer {

  def prettyPrint(results: Traversable[Result]): String = {
    val xml = serialize(results)
    /*  val printer = new scala.xml.PrettyPrinter(120, 4)
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + printer.format(xml)*/
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + xml.toString()
  }

  def serialize(results: Traversable[Result]): Node = {
    <ArrayOfResult xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns="http://lookup.dbpedia.org/">
      { results.map(r => serialize(r)) }
    </ArrayOfResult>
  }

  def serialize(result: Result): Node = {
    <Result>
      <Label>{ result.label }</Label>
      <URI>{ result.uri }</URI>
      <Description>{ result.description }</Description>
      <Classes>{ urisWithLabels(result.classes, "Class") }</Classes>
      <Categories>{ urisWithLabels(result.categories, "Category") }</Categories>
      <Templates>{ uris(result.templates, "Template") }</Templates>
      <Redirects>{ uris(result.redirects, "Redirect") }</Redirects>
      <Refcount>{ result.refCount }</Refcount>
    </Result>
  }

  private def urisWithLabels[A <: Uri with Label](items: Set[A], nodeName: String) = {
    items.map(item => new Elem(null, nodeName, Null, TopScope, <Label>{ item.label }</Label>, <URI>{ item.uri }</URI>))
  }

  private def uris[A <: Uri](items: Set[A], nodeName: String) = {
    items.map(item => new Elem(null, nodeName, Null, TopScope, <URI>{ item.uri }</URI>))
  }

}
