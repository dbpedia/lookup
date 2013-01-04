package org.dbpedia.lookup.entities

import org.scalatest.FunSuite

class EntitiesTest extends FunSuite {

  test("category entity has correct label for uri") {
    val category = new Category("http://dbpedia.org/resource/Category:Berlin")
    assert(category.label == "Berlin", "category label incorrect")
  }

  test("class entity has correct label for uri") {
    val klass = new OntologyClass("http://dbpedia.org/ontology/City")
    assert(klass.label == "city", "class label incorrect")
  }

  test("class entity has correct label for owl#Thing") {
    val klass = new OntologyClass("http://www.w3.org/2002/07/owl#Thing")
    assert(klass.label == "owl#Thing", "class label incorrect")
  }

  test("result entity has correct label for uri") {
    val result = new Result(
      "http://dbpedia.org/resource/Berlin",
      "Some description ...",
      Set[OntologyClass](),
      Set[Category](),
      Set[Template](),
      Set[Redirect](),
      100
    )
    assert(result.label == "Berlin", "result label incorrect")
  }

}
