package org.evomaster.core.search.service.mutator

import org.evomaster.core.database.DbAction
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene

/**
 * created by manzh on 2019-09-10
 *
 * @property mutatedGenes records what genes are mutated
 * @property mutatedDbGenes records what db genes are mutated
 * @property addedGenes (structure mutation) records what genes are added using structure mutator
 * @property removedGene (structure mutation) records what genes are removed using structure mutator
 * @property mutatedPosition records where mutated/added/removed genes are located. but regarding different individual,
 *          the position may be parsed in different way. For instance, the position may indicate the position of resource calls,
 *          not rest action.
 * @property mutatedDbActionPosition records where mutated/added/removed dbgenes are located.
 */
data class MutatedGeneSpecification (
        val mutatedGenes : MutableList<MutatedGene> = mutableListOf(),
        val mutatedDbGenes : MutableList<MutatedGene> = mutableListOf(),
        val addedGenes : MutableList<Gene> = mutableListOf(),
        val removedGene: MutableList<Gene> = mutableListOf(),
        val mutatedPosition : MutableList<Int> = mutableListOf(),
        val mutatedDbActionPosition : MutableList<Int> = mutableListOf(),

        //SQL handling
        val addedInitializationGenes : MutableList<Gene> = mutableListOf(),
        val addedExistingDataInitialization: MutableList<Action> = mutableListOf(),
        val addedInitializationGroup: MutableList<List<Action>> = mutableListOf(),

        //SQL resource handling
        val addedDbActions : MutableList<DbAction> = mutableListOf(),
        val removedDbActions : MutableList<DbAction> = mutableListOf()
){

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

    fun numOfMutatedGeneInfo() = mutatedGenes.size + mutatedDbGenes.size

    fun didAddInitializationGenes() = addedInitializationGenes.isNotEmpty() || addedExistingDataInitialization.isNotEmpty()

    data class MutatedGene(
            val previousValue : String,
            val gene:  Gene
    )

    // add, remove, swap, add_sql, remove_sql
    fun didStructureMutation() = addedGenes.isNotEmpty() || removedGene.isNotEmpty() || (mutatedGenes.isEmpty() && mutatedPosition.isNotEmpty()) || addedDbActions.isNotEmpty() || removedDbActions.isNotEmpty()

    fun allManipulatedGenes() = mutatedDbGenes.plus(mutatedGenes).plus(addedGenes).plus(removedGene)
        .plus(addedDbActions.flatMap { it.seeGenes() }).plus(removedDbActions.flatMap { it.seeGenes() })

    fun mutatedActionOrDb() = setOf(mutatedGenes.isEmpty(), mutatedDbGenes.isNotEmpty())
}