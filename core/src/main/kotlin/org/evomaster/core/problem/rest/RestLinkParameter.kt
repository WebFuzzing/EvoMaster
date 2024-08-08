package org.evomaster.core.problem.rest

import org.evomaster.core.logging.LoggingUtil
import org.slf4j.LoggerFactory

class RestLinkParameter(
    nameEntry: String,
    val value: String
) {

    enum class Scope{
        //https://swagger.io/docs/specification/links/
        //path, query, header or cookie
        PATH, QUERY, HEADER, COOKIE
    }

    companion object{
        private val log = LoggerFactory.getLogger(RestLink::class.java)
    }


    val name: String

    val scope: Scope?

    init{
        name = if(!nameEntry.contains(".")) {
            scope = null
            nameEntry
        } else {
            scope = Scope.valueOf(nameEntry.substringBefore("."))
            nameEntry.substringAfter(".")
        }

        if(!hasUsableValue()){
            LoggingUtil.uniqueWarn(log, "Currently EvoMaster does not handle link parameter values such as: $value")
        }
    }

    fun hasUsableValue(): Boolean {
        //https://swagger.io/docs/specification/links/
        /*
            $url
            $method
            $request.query.param_name
            $request.path.param_name
            $request.header.header_name
            $request.body
            $request.body#/foo/bar
            $statusCode
            $response.header.header_name
            $response.body
            $response.body#/foo/bar
            foo{$request.path.id}bar
         */

        //TODO should handle all cases
        return value.startsWith("\$response.body#")
    }

}