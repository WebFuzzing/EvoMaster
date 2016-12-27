package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class RestPath(val path: String) {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(RestPath::class.java)
    }

    fun resolve(params: List<out Param>) : String {

        val pathParamNames = getPathParamNames()

        var resolvedPath = path

        pathParamNames.forEach { n ->

            var p = params.find{p -> p is PathParam && p.name == n}
            if(p == null){
                log.warn("No path parameter for variable '$n'")

                //this could happen if bug in Swagger
                p = params.find{p -> p is QueryParam && p.name == n}
                if(p == null){
                    throw IllegalArgumentException("Cannot resolve path parameter '$n'")
                }
            }

            val value = p.gene.getValueAsString()

            resolvedPath = resolvedPath.replace("{$n}", value)
        }

        val queries = params.filter { p -> p is QueryParam }
        if(queries.size > 0){
            resolvedPath += "?" +
                    queries.map { q -> q.name+"="+q.gene.getValueAsString() }
                    .joinToString("&")
        }

        return resolvedPath
    }


    fun getPathParamNames() : List<String>{

        val list : MutableList<String> = mutableListOf()
        var open = -1

        for(i in 0 until path.length){
            if(path[i] == '{'){
                open = i
            }
            if(path[i]== '}'){
                open = -1
                list.add(path.substring(open+1, i))
            }
        }

        return list
    }
}