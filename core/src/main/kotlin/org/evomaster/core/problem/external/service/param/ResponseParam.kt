package org.evomaster.core.problem.external.service.param

import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.ObjectGene


class ResponseParam (
    val status: EnumGene<Int> = EnumGene("status", listOf(200, 400, 401, 403, 404, 500)),
    val responses : MutableList<ObjectGene> = mutableListOf(),
    val selected : Int = -1
        ): Param("response", mutableListOf(status).plus(responses).toMutableList()) {

    override fun copyContent(): Param {
        return ResponseParam(status.copy() as EnumGene<Int>, responses.map { it.copy() } as MutableList<ObjectGene>, selected)
    }
}