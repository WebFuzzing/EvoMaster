package org.evomaster.core.search.service.mutator

import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.GeneMutationSelectionMethod

/**
 * created by manzh on 2019-09-10
 *
 * @property mutatedGenes records what genes are mutated
 * @property addedGenes records what genes are added using structure mutator
 * @property removedGene records what genes are removed using structure mutator
 * @property mutatedPosition records where mutated/added/removed genes are located. but regarding different individual,
 * the position may be parsed in different way. For instance, the position may indicate the position of resource calls,
 * not rest action.
 */
open class MutatedGeneSpecification (
        val mutatedGenes : MutableList<MutatedGene> = mutableListOf(),
        val mutatedDbGenes : MutableList<MutatedGene> = mutableListOf(),
        val addedGenes : MutableList<Gene> = mutableListOf(),
        val addedInitializationGenes : MutableList<Gene> = mutableListOf(),
        val removedGene: MutableList<Gene> = mutableListOf(),
        val mutatedPosition : MutableList<Int> = mutableListOf(),
        val mutatedDbActionPosition : MutableList<Int> = mutableListOf()
){
    var geneSelectionStrategy : GeneMutationSelectionMethod = GeneMutationSelectionMethod.NONE

    var mutatedIndividual: Individual? = null
        private set

    fun setMutatedIndividual(individual: Individual){
        if (mutatedIndividual!= null)
            throw IllegalArgumentException("it does not allow setting mutated individual more than one time")
        mutatedIndividual = individual
    }

    fun addMutatedGene(isDb : Boolean, valueBeforeMutation : String, gene : Gene, position : Int?){
        if (isDb){
            mutatedDbGenes.add(MutatedGene(valueBeforeMutation, gene))
            if (position != null) mutatedDbActionPosition.add(position)
        }else{
            mutatedGenes.add(MutatedGene(valueBeforeMutation, gene))
            if (position != null) mutatedPosition.add(position)
        }

    }

    fun mutatedGeneInfo() = mutatedGenes.map { it.gene }
    fun mutatedDbGeneInfo() = mutatedDbGenes.map { it.gene }

    data class MutatedGene(
            val previousValue : String,
            val gene:  Gene
    )
}