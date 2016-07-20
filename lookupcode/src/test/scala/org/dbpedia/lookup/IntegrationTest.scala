package org.dbpedia.lookup

import org.dbpedia.lookup.lucene._
import org.dbpedia.lookup.server._
import com.sun.jersey.api.client._
import net.liftweb.json._
import scala.xml._

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll

/**
 * Full stack test
 *
 * 1. build index
 * 2. Start server
 * 3. Interrogate server via Jersey client over HTTP
 * 4. Kill server, delete index
 */
class IntegrationTest extends FunSuite with BeforeAndAfterAll {

  val tmpDir = TestUtils.tempDirectory
  val port   = TestUtils.tempPort

  var server : Server = _

  override def beforeAll() {
    Indexer.main(Array("src/test/resources"))
    server = new Server(port, new Searcher(tmpDir))
    server.start
  }

  override def afterAll() {
    server.stop
    tmpDir.delete
  }

  def get(path: String, accepts: String = "application/xml") = {
    val client   = new Client
    val resource = client.resource("http://localhost:" + port.toString + path)
    resource.accept(accepts).get(classOf[ClientResponse])
  }

  test("KeywordSearch works") {
    val body   = get("/api/search/KeywordSearch?QueryString=Beirut").getEntity(classOf[String])
    val xml    = XML.loadString(body)
    assert((xml \ "Result" \ "Label").head.text == "Beirut")
    assert((xml \ "Result" \ "Label").tail.head.text == "Beirut (band)")
    assert((xml \ "Result").size == 2)
  }

  test("PrefixSearch works") {
    val body   = get("/api/search/PrefixSearch?QueryString=berl").getEntity(classOf[String])
    val xml    = XML.loadString(body)
    assert((xml \ "Result" \ "Label").head.text == "Berlin")
    assert((xml \ "Result").size == 1)
  }

  test("QueryClass works") {
    val body   = get("/api/search/KeywordSearch?QueryClass=place&QueryString=Beirut").getEntity(classOf[String])
    val xml    = XML.loadString(body)
    assert((xml \ "Result" \ "Label").head.text == "Beirut")
    assert((xml \ "Result").size == 1)
  }

  test("MaxHits works") {
    val body = get("/api/search/KeywordSearch?MaxHits=1&QueryString=beirut").getEntity(classOf[String])
    val xml  = XML.loadString(body)
    assert((xml \ "Result").size == 1)
  }
  
  test("Language works") {
    val body = get("/api/search/KeywordSearch?MaxHits=1&QueryString=beirut&lang=en").getEntity(classOf[String])
    val xml  = XML.loadString(body)
    assert((xml \ "Result" \ "Label").head.text == "Beirut")
    assert((xml \ "Result").size == 1)
  }
  
  test("legacy .asmx in url is optional") {
    assert(get("/api/search.asmx/KeywordSearch").getStatus == 200)
    assert(get("/api/search/KeywordSearch").getStatus == 200)
  }

  test("json results are returned when correct accepts header given") {
    val response = get("/api/search/KeywordSearch", "application/json")
    assert(response.getType.toString == "application/json")
    assert(parse(response.getEntity(classOf[String])) \\ "results" == JArray(List()))
  }

}
