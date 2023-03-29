package org.evomaster.core.problem.rest.resource.dependency

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.service.Randomness

/**
 * to present a chain of actions to create a resource with its dependent resource(s)
 * @property isComplete indicates whether the chain is complete.
 *                  If isComplete is FALSE, it means that actions for creating the resource or dependent resources cannot be found.
 * @property additionalInfo carries info about first resource that cannot be created if isComplete is FALSE, otherwise it is Blank.
 */
abstract class CreationChain(
        private var isComplete : Boolean = false,
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

class PostCreationChain(val actions: MutableList<RestCallAction>, private var failToCreate : Boolean = false) : CreationChain(){
    fun confirmFailure(){
        failToCreate = true
    }

    fun createPostChain(randomness: Randomness) : List<RestCallAction>{
        return actions.map {
            val a = (it.copy() as RestCallAction)
            a.randomize(randomness, false)
            a
        }
    }

    /**
     * add post actions into post resource creation chain at [index]
     */
    fun addActions(index : Int, actionsToAdd: List<RestCallAction>){
        val added = actionsToAdd.filter { actions.none { e-> e.path.toString() == it.path.toString() } }
        actions.addAll(index, added)
    }

    /**
     * @return whether the post resource creation chain already has the [action]
     */
    fun hasAction(action : RestCallAction) : Boolean = actions.any { it.path.toString() == action.path.toString() }

    /**
     * prioritize the sequence of the post actions to prepare the resource
     */
    fun prioritizePostChain(){
        actions.sortBy { it.path.levels() }
    }
}

class DBCreationChain(val actions: MutableList<DbAction>) : CreationChain()

class CompositeCreationChain(val actions: MutableList<Any>) : CreationChain()