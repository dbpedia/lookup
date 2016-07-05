package org.dbpedia.lookup.server

import com.sun.jersey.api.container.httpserver.HttpServerFactory
import com.sun.jersey.api.core.ClassNamesResourceConfig
import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider
import javax.ws.rs.core.Context
import java.net.URI
import org.dbpedia.lookup.lucene.Searcher
import java.io.File
import org.dbpedia.lookup.util.Logging
import io.swagger.jaxrs.config.BeanConfig

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 17.01.11
 * Time: 13:48
 * DBpedia Lookup Server
 */

class SearcherProvider(searcher: Searcher)
  extends SingletonTypeInjectableProvider[Context, Searcher](classOf[Searcher], searcher)

class Server(port: Int, searcher: Searcher) {
  val resources = {
    val config = new ClassNamesResourceConfig(classOf[LookupResource])
    config.getSingletons.add(new SearcherProvider(searcher))
    config

  }

  val serverUri = new URI("http://localhost:" + port.toString + "/")
  val server = HttpServerFactory.create(serverUri, resources)

  def start() {
    server.start()
  }
  def stop() {
    server.stop(0)
  }

}

object Server extends Logging {

  @volatile private var running = true

  def main(args: Array[String]) {
    //get the index dir for all index
    val indexbaseDir = new File(args(0))

    val port = System.getProperty("http.port", "1111").toInt
    val server = new Server(port, new Searcher(indexbaseDir))

    server.start()

    val baseUri = server.serverUri.toString

    logger.info("Server started in " + System.getProperty("user.dir") + " listening on " + baseUri)

    while (running) {
      Thread.sleep(100)
    }

    server.stop()
  }

}
