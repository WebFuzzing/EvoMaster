package org.evomaster.core.search

import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.OptionalGene

object GeneIdUtil{
    private const val SEPARATOR_ACTION_TO_GENE = "::"
    private const val SEPARATOR_ACTION = ";"


    fun generateId(action: Action, gene : Gene) : String = "${action.getName()}$SEPARATOR_ACTION_TO_GENE${gene.name}"

    /*
        LinearIndividual
     */
    fun generateId(gene: Gene) : String = geneId(gene)

    fun extractGeneById(actions: List<Action>, id: String) : MutableList<Gene> {
        val names = id.split(SEPARATOR_ACTION_TO_GENE)
        assert(names.size == 2)
        return actions.filter { it.getName() == names[0] }.flatMap { it.seeGenes() }.filter { it.name == names[1] }.toMutableList()
    }

    fun isAnyChange(geneA : Gene, geneB : Gene) : Boolean{
        assert(geneA::class.java.simpleName == geneB::class.java.simpleName)
        return geneA.getValueAsRawString() == geneB.getValueAsRawString()
    }

    fun geneId(gene: Gene):String{
        return when(gene){
            is DisruptiveGene<*> -> gene.name + SEPARATOR_ACTION_TO_GENE + geneId(gene.gene)
            is OptionalGene -> gene.name + SEPARATOR_ACTION_TO_GENE + geneId(gene.gene)
            else -> gene.name
        }
    }

    fun generateId(individual: Individual) : String = individual.seeActions().map { it.getName() }.joinToString(SEPARATOR_ACTION)
}