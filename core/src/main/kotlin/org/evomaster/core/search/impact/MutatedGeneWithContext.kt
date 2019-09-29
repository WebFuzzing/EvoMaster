package org.evomaster.core.search.impact

import org.evomaster.core.search.gene.Gene

/**
 * this can be used to represent a mutated gene in detail, including
 * @property current gene after mutation
 * @property previous gene before mutation
 * @property action refers to an action which contains the gene
 * @property position indicates where the gene located in a view of an individual, e.g., index of action
 */
class MutatedGeneWithContext (
        val current : Gene,
        val action : String = NO_ACTION,
        val position : Int = NO_POSITION,
        val previous : Gene?
){
    companion object{
        const val NO_ACTION = "NONE"
        const val NO_POSITION = -1
    }
}