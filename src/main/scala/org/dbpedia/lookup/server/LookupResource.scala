package org.dbpedia.lookup.server

import javax.ws.rs._
import core.Response
import org.dbpedia.lookup.entities._

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 17.01.11
 * Time: 13:48
 * Exists only for compatibility to old web service URL (including ".asmx").
 */

@Path("/api/search.asmx")
class LookupResource {

    // Sets the necessary headers in order to enable CORS
    private def ok(accept: String, results: List[Result]): Response = {
        val serializer = (accept contains "application/json") match {
          case true  => new ResultJsonSerializer
          case _     => new ResultXmlSerializer
        }
        Response.ok().entity(serializer.prettyPrint(results)).header("Access-Control-Allow-Origin", "*").build()
    }

    @GET
    @Path("/KeywordSearch")
    @Produces(Array("application/xml", "application/json"))
    def keywordSearch(@DefaultValue("") @HeaderParam("accept") accept: String,
                      @DefaultValue("") @QueryParam("QueryString") query: String,
                      @DefaultValue("") @QueryParam("QueryClass") ontologyClass: String,
                      @DefaultValue("") @QueryParam("MaxHits") maxHitsString: String): Response = {

        val results = Server.searcher.keywordSearch(query, ontologyClass, getMaxHits(maxHitsString))
        System.err.println("KeywordSearch found "+results.length+": MaxHits="+maxHitsString+" QueryClass="+ontologyClass+" QueryString="+query)
        ok(accept, results)
    }

    @GET
    @Path("/PrefixSearch")
    @Produces(Array("application/xml", "application/json"))
    def prefixSearch(@DefaultValue("") @HeaderParam("accept") accept: String,
                     @DefaultValue("") @QueryParam("QueryString") query: String,
                     @DefaultValue("") @QueryParam("QueryClass") ontologyClass: String,
                     @DefaultValue("") @QueryParam("MaxHits") maxHitsString: String): Response = {

        val results = Server.searcher.prefixSearch(query, ontologyClass, getMaxHits(maxHitsString))
        System.err.println("PrefixSearch found "+results.length+": MaxHits="+maxHitsString+" QueryClass="+ontologyClass+" QueryString="+query)
        ok(accept, results)
    }

    private def getMaxHits(maxHitsString: String): Int = {
        try {
            val maxHits = maxHitsString.toInt
            if(maxHits < 0) {
                return Server.MAX_HITS_DEFAULT
            }
            maxHits
        }
        catch {
            case e: NumberFormatException => {
                //System.err.println("WARNING: value of MaxHits must be integer (is '"+maxHitsString+"') setting it to "+MAX_HITS_DEFAULT)
                Server.MAX_HITS_DEFAULT
            }
        }
    }



}
