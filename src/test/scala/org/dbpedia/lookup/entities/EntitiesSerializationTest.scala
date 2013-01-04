package org.dbpedia.lookup.entities

import org.scalatest.FunSuite
import net.liftweb.json._

trait SerializationTest extends FunSuite {

  val template = new Template("http://en.wikipedia.org/wiki/Template:Infobox")

  val redirect = new Redirect("http://en.wikipedia.org/wiki/A_page")

  val klass    = new OntologyClass("http://dbpedia.org/ontology/City")

  val category = new Category("http://dbpedia.org/resource/Category:Berlin")

  val result   = new Result(
     "http://dbpedia.org/resource/Berlin",
     "Berlin is the capital city of Germany & <> ...",
     Set(klass),
     Set(category),
     Set(template),
     Set(redirect),
     100
   )

}

class EntitiesJsonSerializationTest extends SerializationTest {

  val serializer = new ResultJsonSerializer

  test("a list of result entities should serialize to json correctly") {
    implicit val formats = net.liftweb.json.DefaultFormats

    val json = serializer.prettyPrint(List(result, result))
    val data = Serialization.read[Map[String, List[Result]]](json)

    assert(data("results").size == 2)
    assert(data("results").head == result)
  }

}

class EntitiesXmlSerializationTest extends SerializationTest {

  val serializer = new ResultXmlSerializer

  test("a list of result entities should serialize to xml correctly") {
    val xml = serializer.serialize(List(result, result))
    assert((xml \ "Result").size == 2)
  }

  test("the result entity should serialize to XML correctly") {

    val xml = serializer.serialize(result)

    assert((xml \ "Label").text == result.label)
    assert((xml \ "URI").text == result.uri)
    assert((xml \ "Description").text == result.description)
    assert((xml \ "Refcount").text == result.refCount.toString)

    assert((xml \ "Classes" \ "Class" \ "URI").text == result.classes.head.uri)
    assert((xml \ "Classes" \ "Class" \ "Label").text == result.classes.head.label)

    assert((xml \ "Categories" \ "Category" \ "URI").text == result.categories.head.uri)
    assert((xml \ "Categories" \ "Category" \ "Label").text == result.categories.head.label)

    assert((xml \ "Templates" \ "Template" \ "URI").text == result.templates.head.uri)
    assert((xml \ "Redirects" \ "Redirect" \ "URI").text == result.redirects.head.uri)

  }

}
