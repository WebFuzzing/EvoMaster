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


    /**
     * Return a resolved path (starting with "/") based on input parameters.
     * For example:
     *
     * foo/bar/{id}
     *
     * will be resolved into
     *
     * /foo/bar/5
     *
     * if the input params have a variable called "id" with value 5
     */
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

        //make sure to remove unnecessary repeated /
        resolvedPath = resolvedPath.replace("//","/")
        if(! resolvedPath.startsWith("/")){
            resolvedPath = "/" + resolvedPath
        }

        val queries = params.filter { p -> p is QueryParam }
        if(queries.size > 0){
            resolvedPath += "?" +
                    queries.map { q -> q.name+"="+q.gene.getValueAsString() }
                    .joinToString("&")
        }

        return resolvedPath
    }


    private fun getPathParamNames() : List<String>{

        val list : MutableList<String> = mutableListOf()

        var open = -2

        for(i in 0 until path.length){
            if(path[i] == '{'){
                open = i
            }
            if(path[i]== '}'){
                if(open < 0){
                    throw IllegalArgumentException("Closing } was not matched by opening {")
                }

                list.add(path.substring(open+1, i))
                open = -2
            }
        }

        return list
    }
}