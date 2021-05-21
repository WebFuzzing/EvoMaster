package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.Randomness

/**
 *
 */
class BoundGene(name: String, val gene: Gene, val boundGenes : MutableList<Gene>) : Gene(name) {

    override fun copy(): Gene {
        return BoundGene(name, gene.copy(), mutableListOf())
    }

    fun rebuildBindingWithTemplate(newIndividual: Individual, copiedIndividual: Individual, copiedGene: BoundGene){
        if (boundGenes.isNotEmpty())
            throw IllegalArgumentException("gene ($name) has been rebuilt")

        val list = copiedGene.boundGenes.map { g->
            newIndividual.findGene(copiedIndividual, g)
                ?:throw IllegalArgumentException("cannot find the gene (${g.name}) in the copiedIndividual")
        }

        boundGenes.addAll(list)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        gene.randomize(randomness, forceNewValue, allGenes)
        syncWithBoundGenes()
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?
    ): String {
        return gene.getValueAsPrintableString(mode=mode, targetFormat = targetFormat)
    }

    override fun copyValueFrom(other: Gene) {
        TODO("Not yet implemented")
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        TODO("Not yet implemented")
    }

    override fun innerGene(): List<Gene> = listOf(gene)

    /**
     * sync [boundGenes] based on [gene]
     */
    open fun syncWithBoundGenes(){

    }
}