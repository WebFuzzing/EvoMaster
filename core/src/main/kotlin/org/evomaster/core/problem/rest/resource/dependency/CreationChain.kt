package org.evomaster.core.problem.rest.resource.dependency

import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.problem.rest.data.RestCallAction
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

    companion object{
        private val CONFIG_POTENTIAL_REST_ACTION_FOR_CREATION = mapOf(HttpVerb.POST to 0.8, HttpVerb.PUT to 0.2)
    }
    fun confirmFailure(){
        failToCreate = true
    }

    fun createPostChain(randomness: Randomness) : List<RestCallAction>{
        return actions.groupBy { it.path.toString() }.map { (k, v)->
            val creation = if (v.size > 2) {
                throw IllegalStateException("more than 2 potential rest actions (i.e., ${v.size}) for creating $k resource")
            }else if (v.size == 2){
                val prob = CONFIG_POTENTIAL_REST_ACTION_FOR_CREATION[v.first().verb]?:throw IllegalArgumentException("cannot find a probability for a Rest Action with ${v.first().verb} verb to create a resource")
                if (randomness.nextBoolean(prob)){
                    v.first()
                }else
                    v.last()
            }else{
                v.first()
            }
            val a = (creation.copy() as RestCallAction)
            a.randomize(randomness, false)
            a
        }.toList()
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
     * @return whether the post chain contains any action
     */
    fun hasAnyAction() : Boolean = actions.isNotEmpty()

    /**
     * @return size of actions
     */
    fun sizeOfAction() : Int = actions.size

    /**
     * prioritize the sequence of the post actions to prepare the resource
     */
    fun prioritizePostChain(){
        actions.sortBy { it.path.levels() }
    }
}

class DBCreationChain(val actions: MutableList<SqlAction>) : CreationChain()

class CompositeCreationChain(val actions: MutableList<Any>) : CreationChain()