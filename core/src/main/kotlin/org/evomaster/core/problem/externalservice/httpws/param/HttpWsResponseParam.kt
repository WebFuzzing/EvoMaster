package org.evomaster.core.problem.externalservice.httpws.param

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.externalservice.param.ResponseParam
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene


class HttpWsResponseParam (
    /**
     * Contains the values for HTTP status codes.
     * Avoid having 404 as status, since WireMock will return 404 if there is
     * no stub for the specific request.
     */
    val status: EnumGene<Int> = getDefaultStatusEnumGene(),
    /**
     * Response content type, for now supports only JSON
     *
     * TODO might want to support XML and other formats here in the future
     */
    responseType: EnumGene<String> = EnumGene("responseType", listOf("JSON")),
    /**
     * The body payload of the response, if any.
     * Notice that the gene is a String, although the body might NOT be a string, but rather an Object or Array.
     * This still works because we handle it in the phenotype representation, ie using raw values of specializations,
     * which will not be quoted ""
     *
     * TODO: might want to extend StringGene to avoid cases in which taint is lost due to mutation
     */
    responseBody: OptionalGene = OptionalGene(RESPONSE_GENE_NAME, StringGene(RESPONSE_GENE_NAME)),
    val connectionHeader : String? = DEFAULT_HEADER_CONNECTION
): ResponseParam("response", responseType, responseBody, listOf(status)) {

    companion object{
        const val RESPONSE_GENE_NAME = "WireMockResponseGene"
        const val DEFAULT_HEADER_CONNECTION : String = "close"

        fun getDefaultStatusEnumGene() = EnumGene("status", listOf(200, 201, 204, 400, 401, 403, 500))
    }

    override fun copyContent(): Param {
        return HttpWsResponseParam(status.copy() as EnumGene<Int>, responseType.copy() as EnumGene<String>, responseBody.copy() as OptionalGene, connectionHeader)
    }

    fun setStatus(statusCode : Int) : Boolean{
        val index = status.values.indexOf(statusCode)
        if (index == -1) return false
        status.index = index
        return true
    }
}