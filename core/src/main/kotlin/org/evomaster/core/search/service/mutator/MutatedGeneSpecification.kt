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
        val mutatedInitGenes : MutableList<MutatedGene> = mutableListOf(),

        //SQL handling
        val addedInitializationGenes : MutableList<Gene> = mutableListOf(),
        val addedExistingDataInitialization: MutableList<Action> = mutableListOf(),
        val addedInitializationGroup: MutableList<List<Action>> = mutableListOf(),

        //SQL resource handling
        val addedDbActions : MutableList<List<DbAction>> = mutableListOf(),
        val removedDbActions : MutableList<Pair<DbAction, Int>> = mutableListOf()
){

    var mutatedIndividual: Individual? = null
        private set

    fun setMutatedIndividual(individual: Individual){
        if (mutatedIndividual!= null)
            throw IllegalArgumentException("it does not allow setting mutated individual more than one time")
        mutatedIndividual = individual
    }

    fun addMutatedGene(isDb : Boolean, isInit : Boolean, valueBeforeMutation : String, gene : Gene, actionLocalId: String, resourceLocalId: String? = null){
        (if (isInit)
            mutatedInitGenes
        else if (isDb){
            mutatedDbGenes
        }else{
            mutatedGenes
        }).add(MutatedGene(valueBeforeMutation, gene, actionLocalId=actionLocalId, resourceLocalId = resourceLocalId))
    }


    /**
     * record addition and removal action structure mutator
     * @param action to be removed or added
     * @param removed represents if [action] is removed from the individual (true). false means that [action] is added into the individual
     * @param resourceLocalId represents a resource local id of the actions to be removed/added if it has
     *
     */
    fun addRemovedOrAddedByAction(action: Action, removed : Boolean, resourceLocalId: String?){
        mutatedGenes.addAll(
            action.seeTopGenes().map { MutatedGene(null, it, action.getLocalId(),
                    if (removed) MutatedType.REMOVE else MutatedType.ADD, resourceLocalId = resourceLocalId) }
        )
        if (action.seeTopGenes().isEmpty()){
            mutatedGenes.add(MutatedGene(null, null, action.getLocalId(),
                    if (removed) MutatedType.REMOVE else MutatedType.ADD, resourceLocalId = resourceLocalId))
        }
    }


    fun swapAction(resourceLocalId: String?, from: List<String>, to: List<String>){
        mutatedGenes.add(
            MutatedGene(previousValue = null, gene = null, actionLocalId =null, type = MutatedType.SWAP,
                resourceLocalId = resourceLocalId, from = from, to = to)
        )
    }

    /**
     * @return whether the action specified with the [actionLocalId] is mutated
     */
    fun isActionMutated(actionLocalId: String, isInit: Boolean) : Boolean{
        if (isInit)
            return mutatedInitGenes.any { it.type == MutatedType.MODIFY && it.actionLocalId == actionLocalId }

        return (mutatedGenes.plus(mutatedDbGenes)).any { it.type == MutatedType.MODIFY && it.actionLocalId == actionLocalId }
    }

    fun getRemoved(isRest : Boolean) =
        (if (isRest) mutatedGenes else (mutatedInitGenes.plus(mutatedDbGenes))).filter { it.type == MutatedType.REMOVE}

    fun getAdded(isRest : Boolean) = (if (isRest) mutatedGenes else (mutatedInitGenes.plus(mutatedDbGenes))).filter { it.type == MutatedType.ADD}
    fun getMutated(isRest : Boolean) = (if (isRest) mutatedGenes else (mutatedInitGenes.plus(mutatedDbGenes))).filter { it.type == MutatedType.MODIFY}
    fun getSwap() = mutatedGenes.filter { it.type == MutatedType.SWAP }


    fun mutatedGeneInfo() = mutatedGenes.map { it.gene }
    fun mutatedInitGeneInfo() = mutatedInitGenes.map { it.gene }
    fun mutatedDbGeneInfo() = mutatedDbGenes.map { it.gene }

    fun numOfMutatedGeneInfo() = mutatedGenes.size + mutatedDbGenes.size+ mutatedInitGenes.size

    fun didAddInitializationGenes() = addedInitializationGenes.isNotEmpty() || addedExistingDataInitialization.isNotEmpty()

    data class MutatedGene(
        val previousValue : String? = null,
        val gene:  Gene?,
        val actionLocalId: String?,
        val type : MutatedType = MutatedType.MODIFY,
        val resourceLocalId : String?,
        val from : List<String>? = null,
        val to : List<String>? = null
    )

    enum class MutatedType{
        ADD,
        REMOVE,
        MODIFY,
        SWAP
    }

    // add, remove, swap, add_sql, remove_sql
    fun didStructureMutation() =  mutatedGenes.any { it.type != MutatedType.MODIFY }
            || (mutatedInitGenes.plus(mutatedDbGenes)).any { it.type != MutatedType.MODIFY }
            || addedDbActions.isNotEmpty() || removedDbActions.isNotEmpty()

    fun isMutated(gene: Gene) = (mutatedInitGenes.plus(mutatedDbGenes)).any { it.gene == gene }
            || mutatedGenes.any { it.gene == gene }
            || addedDbActions.flatten().any { it.seeTopGenes().contains(gene) }
            || removedDbActions.map { it.first }.any { it.seeTopGenes().contains(gene) }

    fun mutatedActionOrInit() = setOf((mutatedGenes.plus(mutatedDbGenes)).isEmpty(), mutatedInitGenes.isNotEmpty())
}