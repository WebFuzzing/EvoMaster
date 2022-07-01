package org.evomaster.core.problem.external.service

import com.github.tomakehurst.wiremock.WireMockServer
import org.evomaster.core.search.Action
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

class ExternalServiceAction (
    val request: ExternalServiceRequest,
    val wireMockServer: WireMockServer,
    private val id: Long,
    computedGenes: List<Gene>? = null,
        ) : Action(listOf()) {

    private val genes: List<Gene> = (computedGenes ?:
        ExternalServiceActionGeneBuilder().buildGene(request)
    )

    override fun getName(): String {
        // TODO: Need to change in future
        return request.getID()
    }

    override fun seeGenes(): List<out Gene> {
        return genes
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return false
    }

    override fun copyContent(): StructuralElement {
        return ExternalServiceAction(request, wireMockServer, id, genes.map(Gene::copy))
    }

}