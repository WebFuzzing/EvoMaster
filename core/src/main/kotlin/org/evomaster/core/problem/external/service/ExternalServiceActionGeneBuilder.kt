package org.evomaster.core.problem.external.service

import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene

/**
 * Gene builder to handle external service calls
 */
class ExternalServiceActionGeneBuilder {

    /**
     * will return Gene to represent the responses
     */
    fun buildGene(request: ExternalServiceRequest): List<Gene> {
        return listOf(EnumGene(name = request.getID(), data = setOf(200, 400, 401, 404, 500)))
    }
}