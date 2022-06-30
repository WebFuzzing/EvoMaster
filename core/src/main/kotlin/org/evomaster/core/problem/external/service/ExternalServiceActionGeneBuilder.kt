package org.evomaster.core.problem.external.service

import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene

class ExternalServiceActionGeneBuilder {

    fun buildGene(request: ServeEvent): List<Gene> {
        return listOf(EnumGene(name = request.id.toString(), data = setOf(200, 404, 500)))
    }
}