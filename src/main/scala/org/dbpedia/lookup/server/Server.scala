package org.dbpedia.lookup.server

import com.sun.jersey.api.container.httpserver.HttpServerFactory
import com.sun.jersey.api.core.ClassNamesResourceConfig
import java.net.URI
import org.dbpedia.lookup.lucene.Searcher

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 17.01.11
 * Time: 13:48
 * DBpedia Lookup Server
 */

object Server {

    val serverURI = new URI("http://localhost:1111/")

    val MAX_HITS_DEFAULT = 5

    private val resources = new ClassNamesResourceConfig(classOf[LookupResource])

    protected[server] val searcher = new Searcher()

    @volatile private var running = true

    def main(args : Array[String]) {

        val server = HttpServerFactory.create(serverURI, resources)
        server.start()

        System.err.println("Server started in " + System.getProperty("user.dir") + " listening on " + serverURI)

        //Open browser
        try {
            val example = new URI(serverURI.toString+"api/search.asmx/KeywordSearch?QueryString=Berlin")
            java.awt.Desktop.getDesktop().browse(example)
        }
        catch {
            case e : Exception => System.err.println("Could not open browser. ", e)
        }

        while(running) {
            Thread.sleep(100)
        }

        //Stop the HTTP server
        server.stop(0)
    }

}
