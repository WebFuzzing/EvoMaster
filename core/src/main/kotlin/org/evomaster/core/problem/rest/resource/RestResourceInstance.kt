package org.evomaster.core.problem.rest.resource

import org.evomaster.core.problem.rest.param.Param

class RestResourceInstance (val referResourceNode : RestResourceNode, val params: List<Param>){

    fun equals(others : List<Param>) : Boolean{
        return referResourceNode.path.resolve(params) == referResourceNode.path.resolve(others)
    }

    fun getKey() : String{
        return referResourceNode.path.resolve(params)
    }

    fun getAResourceKey() : String = referResourceNode.path.toString()

    fun copy() : RestResourceInstance{//keep same referResource, but copy new param
        return RestResourceInstance(referResourceNode, params.map { param -> param.copy() })
    }
}