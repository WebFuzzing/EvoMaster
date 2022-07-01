package org.evomaster.core.problem.external.service

import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene

class ExternalServiceActionGeneBuilder {

    fun buildGene(request: ExternalServiceRequest): List<Gene> {
        return listOf(EnumGene(name = request.getID(), data = setOf(200, 404, 500)))
    }
}