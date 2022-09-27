package org.evomaster.core.problem.external.service.httpws.param

import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.problem.external.service.param.ResponseParam
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.optional.OptionalGene


class HttpWsResponseParam (
    /**
     * Contains the values for HTTP status codes.
     * Avoid having 404 as status, since WireMock will return 404 if there is
     * no stub for the specific request.
     */
    val status: EnumGene<Int> = EnumGene("status", listOf(200, 400, 401, 403, 500)),
    /**
     * Response content type, for now supports only JSON
     *
     * TODO might want to support XML and other formats here in the future
     */
    responseType: EnumGene<String> = EnumGene("responseType", listOf("JSON")),
    response: OptionalGene = OptionalGene("response", ObjectGene("response", listOf()))
): ResponseParam("response", responseType, response, listOf(status)) {

    override fun copyContent(): Param {
        return HttpWsResponseParam(status.copy() as EnumGene<Int>, responseType.copy() as EnumGene<String>, response.copy() as OptionalGene)
    }

    fun getStatus() : String {
        return status.getValueAsRawString()
    }
}