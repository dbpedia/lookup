package org.dbpedia.lookup.server

import javax.ws.rs._
import javax.ws.rs.core.Context
import core.Response
import org.dbpedia.lookup.entities._
import org.dbpedia.lookup.lucene.Searcher
import org.dbpedia.lookup.util.Logging

/**
 * Controller for DBpedia Lookup web service.
 */
@Path("/api/search{ext:(.asmx)?}")
@Produces(Array("application/xml", "application/json"))
class LookupResource extends Logging {

    @Context
    var searcher : Searcher = _

    @DefaultValue("") @HeaderParam("accept")
    var accept   : String   = _

    @DefaultValue("") @QueryParam("QueryString")
    var query    : String   = _

    @DefaultValue("") @QueryParam("QueryClass")
    var ontologyClass : String = _

    @DefaultValue("5") @QueryParam("MaxHits")
    var maxHits : Int = _

    @GET
    @Path("/KeywordSearch")
    def keywordSearch : Response = {
        val results = searcher.keywordSearch(query, ontologyClass, maxHits)
        logger.info("KeywordSearch found "+results.length+": MaxHits="+maxHits.toString+" QueryClass="+ontologyClass+" QueryString="+query)
        ok(results)
    }

    @GET
    @Path("/PrefixSearch")
    def prefixSearch : Response = {
        val results = searcher.prefixSearch(query, ontologyClass, maxHits)
        logger.info("PrefixSearch found "+results.length+": MaxHits="+maxHits.toString+" QueryClass="+ontologyClass+" QueryString="+query)
        ok(results)
    }

    // Sets the necessary headers in order to enable CORS
    private def ok(results: List[Result]): Response = {
        Response.ok().entity(serialize(results)).header("Access-Control-Allow-Origin", "*").build()
    }

    private def serialize(results: List[Result]): String = {
        val serializer = (accept contains "application/json") match {
            case true  => new ResultJsonSerializer
            case _     => new ResultXmlSerializer
        }
        serializer.prettyPrint(results)
    }

}
