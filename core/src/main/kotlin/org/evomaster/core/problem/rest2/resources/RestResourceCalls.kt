package org.evomaster.core.problem.rest.serviceII.resources

import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestCallAction

class RestResourceCalls(val resource: RestResource, val actions: MutableList<RestAction>){

    fun copy() : RestResourceCalls{
        return RestResourceCalls(resource.copy(), actions.map { a -> a.copy() as RestAction}.toMutableList())
    }

    fun copy(other: MutableList<RestAction>) : RestResourceCalls{
        return RestResourceCalls(resource.copy(), actions.map { a -> other.find { o -> a.getName() == o.getName() }!!}.toMutableList())
    }

    fun update(){
        resource.ar.actions.filter { a ->
            a is RestCallAction && actions.find { a.getName() == it.getName()}?.seeGenes()?.size != a.seeGenes().size
        }.forEach { a->
            val template = actions.find { a.getName() == it.getName()}!!

        }
    }
}