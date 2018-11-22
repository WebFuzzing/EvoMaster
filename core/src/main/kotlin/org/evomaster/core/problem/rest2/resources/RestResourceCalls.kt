package org.evomaster.core.problem.rest.serviceII.resources

import org.evomaster.core.problem.rest.RestAction

class RestResourceCalls(val resource: RestResource, val actions: MutableList<RestAction>){

    fun copy() : RestResourceCalls{
        return RestResourceCalls(resource.copy(), actions.map { a -> a.copy() as RestAction}.toMutableList())
    }

    fun copy(other: MutableList<RestAction>) : RestResourceCalls{
        return RestResourceCalls(resource.copy(), actions.map { a -> other.find { o -> a.getName() == o.getName() }!!}.toMutableList())
    }

}