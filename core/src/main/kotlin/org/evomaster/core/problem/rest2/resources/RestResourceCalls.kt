package org.evomaster.core.problem.rest.serviceII.resources

import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.gene.Gene

class RestResourceCalls(val resource: RestResource, val actions: MutableList<RestAction>){

    fun copy() : RestResourceCalls{
        return RestResourceCalls(resource.copy(), actions.map { a -> a.copy() as RestAction}.toMutableList())
    }

    fun copy(other: MutableList<RestAction>) : RestResourceCalls{
        return RestResourceCalls(resource.copy(), actions.map { a -> other.find { o -> a.getName() == o.getName() }!!}.toMutableList())
    }

    //return genes of the first rest action
    fun seeGeens() : List<out Gene>{
        return actions.first().seeGenes()
    }

    fun repairGenesAfterMutation(){
        //bind all actions regarding first action
        val first = actions.first() as RestCallAction
        (1 until actions.size).forEach { i->
            if(actions[i] is RestCallAction){
                (actions[i] as RestCallAction).bindToSamePathResolution(first)
            }
        }
    }
}