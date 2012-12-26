package org.dbpedia.lookup.entities

import scala.xml._

class ResultXmlSerializer {

  def serialize(results : Traversable[Result]) : Node = {
    <ArrayOfResult xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                   xmlns="http://lookup.dbpedia.org/">
      { results.map(r => serialize(r)) }
    </ArrayOfResult>
  }

  def serialize(result : Result) : Node = {
    <Result>
      <Label>{result.label}</Label>
      <URI>{result.uri}</URI>
      <Description>{result.description}</Description>
      <Classes>{ urisWithLabels(result.classes, "Class") }</Classes>
      <Categories>{ urisWithLabels(result.categories, "Category") }</Categories>
      <Templates>{ uris(result.templates, "Template") }</Templates>
      <Redirects>{ uris(result.redirects, "Redirect") }</Redirects>
      <Refcount>{ result.refCount }</Refcount>
    </Result>
  }

  private def urisWithLabels[A <: Uri with Label](items: Set[A], nodeName: String) = {
    items.map(item => new Elem(null, nodeName, Null, TopScope, <Label>{item.label}</Label>, <URI>{item.uri}</URI>))
  }

  private def uris[A <: Uri](items: Set[A], nodeName: String) = {
    items.map(item => new Elem(null, nodeName, Null, TopScope, <URI>{item.uri}</URI>))
  }

}
