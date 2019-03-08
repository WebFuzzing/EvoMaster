package org.evomaster.core.problem.rest2.resources.dependency

import org.evomaster.core.search.Action

/**
 * to present a chain of actions to create a resource with its dependent resource(s)
 * @property isComplete indicates whether the chain is complete.
 *                  If isComplete is FALSE, it means that actions for creating the resource or dependent resources cannot be found.
 * @property additionalInfo carries info about first resource that cannot be created if isComplete is FALSE, otherwise it is Blank.
 */
open class CreationChain(
        val actions: MutableList<Action>,
        private var isComplete : Boolean,
        var additionalInfo : String = ""
){
    fun confirmComplete(){
        isComplete = true
    }

    fun confirmIncomplete(){
        isComplete = false
    }

    fun confirmIncomplete(info : String){
        isComplete = false
        additionalInfo = info
    }

    fun isComplete() :Boolean = isComplete
}

class PossibleCreationChain(
        val actions: MutableList<Action>,
        var probability : Double
){
    init {
        assert(probability in 0.0..1.0)
    }
}