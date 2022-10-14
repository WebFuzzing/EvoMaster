package org.evomaster.core.problem.external.service.httpws.param

import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.problem.external.service.param.ResponseParam
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene


class HttpWsResponseParam (
    /**
     * Contains the values for HTTP status codes.
     * Avoid having 404 as status, since WireMock will return 404 if there is
     * no stub for the specific request.
     */
    val status: EnumGene<Int> = EnumGene("status", listOf(200, 201, 204, 400, 401, 403, 500)),
    /**
     * Response content type, for now supports only JSON
     *
     * TODO might want to support XML and other formats here in the future
     */
    responseType: EnumGene<String> = EnumGene("responseType", listOf("JSON")),
    /**
     * The body payload of the response, if any
     *
     * TODO: might want to extend StringGene to avoid cases in which taint is lost due to mutation
     */
    responseBody: OptionalGene = OptionalGene(RESPONSE_GENE_NAME, StringGene(RESPONSE_GENE_NAME)),
    val connectionHeader : String? = DEFAULT_HEADER_CONNECTION
): ResponseParam("response", responseType, responseBody, listOf(status)) {

    companion object{
        const val RESPONSE_GENE_NAME = "WireMockResponseGene"
        const val DEFAULT_HEADER_CONNECTION : String = "close"
    }

    override fun copyContent(): Param {
        return HttpWsResponseParam(status.copy() as EnumGene<Int>, responseType.copy() as EnumGene<String>, responseBody.copy() as OptionalGene, connectionHeader)
    }
}