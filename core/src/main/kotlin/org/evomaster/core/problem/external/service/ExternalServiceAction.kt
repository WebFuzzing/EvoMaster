package org.evomaster.core.problem.external.service

import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import org.evomaster.core.search.Action
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness

class ExternalServiceAction (
    val request: ServeEvent,
    private val id: Long,
    computedGenes: List<Gene>? = null,
        ) : Action(listOf()) {

    private val genes: List<Gene> = (computedGenes ?:
        ExternalServiceActionGeneBuilder().buildGene(request)
    )

    override fun getName(): String {
        // TODO: Need to change in future
        return request.id.toString()
    }

    override fun seeGenes(): List<out Gene> {
        return genes
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return false
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, all: List<Action>) {
        val allGenes = all.flatMap { it.seeGenes() }
        seeGenes().asSequence()
            .filter { it.isMutable() }
            .forEach {
                it.randomize(randomness, false, allGenes)
            }
    }

    override fun getChildren() : List<Gene> = genes

    override fun copyContent(): StructuralElement {
        return ExternalServiceAction(request, id, genes.map(Gene::copyContent))
    }
}