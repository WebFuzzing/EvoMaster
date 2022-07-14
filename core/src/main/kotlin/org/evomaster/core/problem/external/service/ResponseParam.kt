package org.evomaster.core.problem.external.service

import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.ObjectGene


class ResponseParam (
    val status: EnumGene<Int> = EnumGene("status", listOf(200, 404)),
    val responses : MutableList<ObjectGene> = mutableListOf(),
    val selected : Int = -1
        ): Param("response", mutableListOf(status).plus(responses).toMutableList()) {
}