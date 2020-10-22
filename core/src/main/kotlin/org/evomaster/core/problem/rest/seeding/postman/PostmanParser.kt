package org.evomaster.core.problem.rest.seeding.postman

import com.google.gson.Gson
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.problem.rest.seeding.Parser
import org.evomaster.core.problem.rest.seeding.postman.pojos.PostmanCollectionObject
import org.evomaster.core.problem.rest.seeding.postman.pojos.Request
import org.evomaster.core.problem.rest.service.RestSampler
import org.evomaster.core.search.gene.Gene
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.StandardCharsets

class PostmanParser(
        private val restSampler: RestSampler
) : Parser {

    private val swagger = restSampler.getOpenAPI()

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PostmanParser::class.java)
    }

    override fun parseTestCases(path: String): MutableList<RestIndividual> {
        var testCases = mutableListOf<RestIndividual>()

        val postmanContent = File(path).inputStream().readBytes().toString(StandardCharsets.UTF_8)
        val postmanObject = Gson().fromJson(postmanContent, PostmanCollectionObject::class.java)

        val defaultRestActions = restSampler.seeAvailableActions().filterIsInstance<RestCallAction>()

        postmanObject.item.forEach { postmanItem ->
            val postmanRequest = postmanItem.request

            // Get action corresponding to Postman request
            val restAction = getRestAction(defaultRestActions, postmanRequest)

            restAction?.parameters?.forEach { parameter ->
                updateParameterGenesWithRequest(parameter, postmanRequest)
            }

        }

        return testCases
    }

    private fun getRestAction(defaultRestActions: List<RestCallAction>, postmanRequest: Request): RestCallAction? {
        val baseUrl = swagger.servers[0].url
        val verb = postmanRequest.method
        val path = postmanRequest.url.raw.removePrefix(baseUrl).split('?')[0]
        val originalRestAction = defaultRestActions.firstOrNull { it.verb.toString() == verb && it.path.matches(path) }

        if (originalRestAction == null)
            log.warn("Endpoint {} not found in the Swagger", "$verb:$path")

        return originalRestAction?.copy() as RestCallAction?
    }

    private fun updateParameterGenesWithRequest(parameter: Param, postmanRequest: Request) {
        val paramName = parameter.name
        val paramType = parameter.javaClass
        val rootGene = parameter.gene

        val paramValue = getParamValueFromRequest(parameter, postmanRequest)


//        when(parameter) {
//            is HeaderParam, is QueryParam, is BodyParam ->
//        }
    }

    /**
     * In a Postman collection file, all parameter values are represented as strings,
     * even if it is a JSON body.
     *
     * @param parameter Parameter extracted from an action
     * @param postmanRequest Postman representation of a request
     * @return Value of the parameter in the Postman request, null if not found
     */
    private fun getParamValueFromRequest(parameter: Param, postmanRequest: Request): String? {
        var value: String? = null
        when (parameter) {
            is HeaderParam -> value = postmanRequest.header?.find { it.key == parameter.name }?.value
//                    postmanRequest.header.first { it.key == parameter.name }.value
        }

        return value
    }

}