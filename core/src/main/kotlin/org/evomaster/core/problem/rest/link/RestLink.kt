package org.evomaster.core.problem.rest.link

import org.evomaster.core.logging.LoggingUtil
import org.slf4j.LoggerFactory

/**
 * Object representing a REST link from OpenAPI schema definition.
 *
 * https://swagger.io/docs/specification/links/
 *
 * This object should be immutable
 */
class RestLink(
    /**
     * Links are defined for responses, based on their status code.
     * For example, a link could only make sense for a 200, and undefined/useless for a 500.
     * For same code, several links different operations could be defined
     */
    val statusCode: Int,
    val name: String,
    val operationId: String?,
    val operationRef: String?,
    parameterDefinitions: Map<String,String>,
    val requestBody: String?,
    val server: String?
) {

    companion object{
        private val log = LoggerFactory.getLogger(RestLink::class.java)
    }

    val id = "$statusCode:$name"

    val parameters : List<RestLinkParameter>

    init {
        if (operationId == null && operationRef == null) {
            throw IllegalArgumentException("In link named '$name' the operationId and operationRef cannot be both null")
        }
        if(!operationId.isNullOrEmpty() && !operationRef.isNullOrEmpty()) {
            throw IllegalArgumentException("In link named '$name' the operationId and operationRef cannot be both defined")
        }
        parameters = parameterDefinitions.map { RestLinkParameter(it.key, it.value) }
    }


    /**
     * Not all link definitions can be used.
     * For example, refs to external services are unnecessary for the fuzzing of the SUT.
     * Furthermore, the support for links in EM might not be fully completed yet.
     */
    fun canUse() : Boolean{
        if(operationRef != null && !operationRef.startsWith("#")){
            //only local refs make sense
            return false
        }

        if(!operationRef.isNullOrEmpty()){
            LoggingUtil.uniqueWarn(log, "Link '$name': currently not supporting yet operationRef")
            //TODO needs to be implemented
            return false
        }

        if(server != null){
            //requests to external servers make little sense
            return false
        }

        if(!requestBody.isNullOrEmpty()){
            LoggingUtil.uniqueWarn(log, "Link '$name': currently not supporting yet requestBody")
            /*
                unclear if this is of any use, at least for the moment. see:
                https://swagger.io/docs/specification/links/
                TODO: keep this in mind for future versions of OAS
             */
            return false

        }

        if(parameters.none { it.hasUsableValue() }){
            LoggingUtil.uniqueWarn(log, "Link '$name': currently can't handle any of its parameter definitions")
            return false
        }

        return true
    }

    fun needsToUseResponse() : Boolean{
        //TODO expand when supporting other types as well
        return parameters.any { it.isBodyField() }
    }
}