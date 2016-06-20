package org.dbpedia.lookup.server

import javax.ws.rs._
import javax.ws.rs.core.Context
import core.Response
import org.dbpedia.lookup.entities._
import org.dbpedia.lookup.lucene.Searcher
import org.dbpedia.lookup.util.Logging
import io.swagger.annotations._

/**
 * Controller for DBpedia Lookup web service.

 */
@Path("/api/search{ext:(.asmx)?}")
@Produces(Array("application/xml", "application/json", "application/json+ld"))
@Api(value="/api/search{ext:(.asmx)?}", description = "Endpoint to Search with Label")
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
    @ApiOperation(value = "Gets a list of  resource.")//, response = classOf[Result], responseContainer = "list")
    @ApiResponses(Array(
        new ApiResponse(code = 200, message = "Keyword Found!!"),
        new ApiResponse(code = 404, message = " Keyword resource not found")
    ))
    def keywordSearch : Response = {
        val results = searcher.keywordSearch(query, ontologyClass, maxHits)
        logger.info("KeywordSearch found "+results.length+": MaxHits="+maxHits.toString+" QueryClass="+ontologyClass+" QueryString="+query)
        ok(results)
    }

    @GET
    @Path("/PrefixSearch")
    @ApiOperation(value = "Gets a list of  resource.")//, response = classOf[Result], responseContainer = "list")
    @ApiResponses(Array(
        new ApiResponse(code = 200, message = "Keyword Found!!"),
        new ApiResponse(code = 404, message = " Keyword resource not found")
    ))
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
        val serializer = (accept) match {
            case "application/json"  => new ResultJsonSerializer
            case "application/json+ld"  => new ResultJsonLDSerializer
            case _     => new ResultXmlSerializer
        }

        serializer.prettyPrint(results)
    }

}
