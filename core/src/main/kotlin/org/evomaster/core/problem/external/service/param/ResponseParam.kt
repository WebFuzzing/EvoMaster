package org.evomaster.core.problem.external.service.param

import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.OptionalGene


class ResponseParam (
    /**
     * Contains the values for HTTP status codes
     */
    val status: EnumGene<Int> = EnumGene("status", listOf(200, 400, 401, 403, 404, 500)),
    /**
     * Response content type, for now supports only JSON
     */
    val responseType: EnumGene<String> = EnumGene("responseType", listOf("JSON")),
    val response: OptionalGene = OptionalGene("response", ObjectGene("response", listOf()))
        ): Param("response", mutableListOf(status).plus(responseType).toMutableList()) {

    override fun copyContent(): Param {
        return ResponseParam(status.copy() as EnumGene<Int>, responseType.copy() as EnumGene<String>, response.copy() as OptionalGene)
    }
}