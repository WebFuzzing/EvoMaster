package org.evomaster.core.problem.rest.link

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.slf4j.LoggerFactory

class RestLinkParameter(
    nameEntry: String,
    val value: String
) {

    enum class Scope{
        //https://swagger.io/docs/specification/links/
        //path, query, header or cookie
        PATH, QUERY, HEADER, COOKIE;

        fun matchType(param: Param): Boolean {
            return when(this){
                PATH -> param is PathParam
                QUERY -> param is QueryParam
                HEADER -> param is HeaderParam
                COOKIE -> param is HeaderParam && param.name.equals("cookie", ignoreCase = true)
            }
        }
    }

    companion object{
        private val log = LoggerFactory.getLogger(RestLink::class.java)

        private const val BODY_FIELD = "\$response.body#"
    }


    val name: String

    val scope: Scope?

    init{
        name = if(!nameEntry.contains(".")) {
            scope = null
            nameEntry
        } else {
            scope = Scope.valueOf(nameEntry.substringBefore(".").uppercase())
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
        return isBodyField() || isConstant()
    }

    fun isBodyField() = value.startsWith(BODY_FIELD)

    fun bodyPointer(): String {
        if(!isBodyField()){
            throw IllegalStateException("There is no body pointer to extract")
        }
        return value.substringAfter("#")
    }

    fun isConstant() = !value.startsWith("$") && !value.contains("{$")
}