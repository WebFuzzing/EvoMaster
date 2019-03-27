package org.evomaster.core.problem.rest.serviceII.resources

import org.evomaster.core.problem.rest.param.Param

class RestResource (val ar : RestAResource, val params: List<Param>){

    fun equals(others : List<Param>) : Boolean{//FIXME
        return ar.path.resolve(params) == ar.path.resolve(others)
    }

    fun getKey() : String{
        return ar.path.resolve(params)
    }

    fun getAResourceKey() : String = ar.path.toString()

    fun copy() : RestResource{//keep same ar, but copy new param
        return RestResource(ar, params.map { param -> param.copy() })
    }
}