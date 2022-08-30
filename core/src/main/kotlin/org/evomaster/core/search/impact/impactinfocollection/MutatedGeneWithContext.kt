package org.evomaster.core.search.impact.impactinfocollection

import org.evomaster.core.search.gene.Gene

/**
 * this can be used to represent a mutated gene in detail, including
 * @property current gene after mutation
 * @property previous gene before mutation
 * @property action refers to an action which contains the gene
 * @property actionLocalId represents a local id of the action in the individual
 */
class MutatedGeneWithContext (
        val current : Gene,
        val action : String = NO_ACTION,
        val actionLocalId : String = NO_ACTION,
        val previous : Gene?,
        val numOfMutatedGene: Int = 1
){
    companion object{
        const val NO_ACTION = "NONE"
    }

    fun mainPosition(current: Gene, previous: Gene?, numOfMutatedGene: Int) : MutatedGeneWithContext{
        return MutatedGeneWithContext(current = current, previous = previous,action = this.action, actionLocalId = this.actionLocalId, numOfMutatedGene = numOfMutatedGene)
    }
}