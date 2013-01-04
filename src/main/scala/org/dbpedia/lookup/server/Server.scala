package org.dbpedia.lookup.server

import com.sun.jersey.api.container.httpserver.HttpServerFactory
import com.sun.jersey.api.core.ClassNamesResourceConfig
import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider
import javax.ws.rs.core.Context
import java.net.URI
import org.dbpedia.lookup.lucene.Searcher
import org.dbpedia.lookup.util.Logging

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
        val config   = new ClassNamesResourceConfig(classOf[LookupResource])
        config.getSingletons.add(new SearcherProvider(searcher))
        config
    }

    val serverUri = new URI("http://localhost:" + port.toString + "/")
    val server    = HttpServerFactory.create(serverUri, resources)

    def start = server.start
    def stop  = server.stop(0)

}

object Server extends Logging {

    @volatile private var running = true

    def main(args : Array[String]) {

        val port   = System.getProperty("http.port", "1111").toInt
        val server = new Server(port, new Searcher)

        server.start

        val baseUri = server.serverUri.toString

        logger.info("Server started in " + System.getProperty("user.dir") + " listening on " + baseUri)

        //Open browser
        try {
            val example = new URI(baseUri + "api/search/KeywordSearch?QueryString=Berlin")
            java.awt.Desktop.getDesktop().browse(example)
        }
        catch {
            case e : Exception => logger.error("Could not open browser. ", e)
        }

        while(running) {
            Thread.sleep(100)
        }

        //Stop the HTTP server
        server.stop
    }

}
