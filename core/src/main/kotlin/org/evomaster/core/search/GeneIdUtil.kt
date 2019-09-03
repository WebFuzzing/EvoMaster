package org.evomaster.core.search

import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.OptionalGene
import java.lang.IllegalArgumentException

/**
 * manage an id of genes in order to identify evolution of genes along with evolution of individuals
 */
object GeneIdUtil{
    private const val SEPARATOR_ACTION_TO_GENE = "::"
    private const val SEPARATOR_GENE = ";"

    private const val SEPARATOR_GENE_WITH_TYPE = ">"


    fun generateGeneId(action: Action, gene : Gene) : String = "${action.getName()}$SEPARATOR_ACTION_TO_GENE${generateGeneId(gene)}"

    fun <T : Individual> generateGeneId(individual: T, gene: Gene) : String{
        if (!individual.seeGenes().contains(gene))
            throw IllegalArgumentException("cannot find this gene in this individual")
        individual.seeActions().find { a-> a.seeGenes().contains(gene) }?.let {
            return generateGeneId(it, gene)
        }
        return generateGeneId(gene)
    }

    fun extractGeneById(actions: List<Action>, id: String) : MutableList<Gene>{
        if (actions.isEmpty() || id.contains(SEPARATOR_ACTION_TO_GENE)) return mutableListOf()

        val names = id.split(SEPARATOR_ACTION_TO_GENE)

        assert(names.size == 2)
        return actions.filter { it.getName() == names[0] }.flatMap { it.seeGenes() }.filter { it.name == names[1] }.toMutableList()
    }

    fun isAnyChange(geneA : Gene, geneB : Gene) : Boolean{
        assert(geneA::class.java.simpleName == geneB::class.java.simpleName)
        return geneA.getValueAsRawString() == geneB.getValueAsRawString()
    }

    /**
     * TODO: should handle SQL related genes separately?
     */
    fun generateGeneId(gene: Gene):String{
        return when(gene){
            is DisruptiveGene<*> -> gene.name + SEPARATOR_GENE_WITH_TYPE + generateGeneId(gene.gene)
            is OptionalGene -> gene.name + SEPARATOR_GENE_WITH_TYPE + generateGeneId(gene.gene)
            is ObjectGene -> if (gene.refType.isNullOrBlank()) gene.name else "${gene.refType}$SEPARATOR_GENE_WITH_TYPE${gene.name}"
            else -> gene.name
        }
    }

    /**
     * TODO: handle SQL Actions in an individual
     */
    fun generateIndividualId(individual: Individual) : String = individual.seeActions().joinToString(SEPARATOR_GENE) { it.getName() }
}