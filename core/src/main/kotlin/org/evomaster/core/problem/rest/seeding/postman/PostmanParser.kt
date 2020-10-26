package org.evomaster.core.problem.rest.seeding.postman

import com.google.gson.Gson
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.problem.rest.seeding.AbstractParser
import org.evomaster.core.problem.rest.seeding.postman.pojos.PostmanCollectionObject
import org.evomaster.core.problem.rest.seeding.postman.pojos.Request
import org.evomaster.core.search.gene.Gene
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets

class PostmanParser(
        defaultRestCallActions: List<RestCallAction>,
        swagger: OpenAPI
) : AbstractParser(defaultRestCallActions, swagger) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PostmanParser::class.java)
    }

    override fun parseTestCases(path: String): MutableList<MutableList<RestCallAction>> {
        val testCases = mutableListOf<MutableList<RestCallAction>>()

        val postmanContent = File(path).inputStream().readBytes().toString(StandardCharsets.UTF_8)
        val postmanObject = Gson().fromJson(postmanContent, PostmanCollectionObject::class.java)

        postmanObject.item.forEach { postmanItem ->
            val postmanRequest = postmanItem.request

            // Copy action corresponding to Postman request
            val restAction = getRestAction(defaultRestCallActions, postmanRequest)

            if (restAction != null) {
                // Update action parameters according to Postman request
                restAction.parameters.forEach { parameter ->
                    updateParameterGenesWithRequest(parameter, postmanRequest, restAction)
                }

                testCases.add(mutableListOf(restAction))
            }
        }

        return testCases
    }

    private fun getRestAction(defaultRestActions: List<RestCallAction>, postmanRequest: Request): RestCallAction? {
        val verb = postmanRequest.method
        val path = URI(postmanRequest.url.raw).path.trim()
        val originalRestAction = defaultRestActions.firstOrNull { it.verb.toString() == verb && it.path.matches(path) }

        if (originalRestAction == null)
            log.warn("Endpoint {} not found in the Swagger", "$verb:$path")

        return originalRestAction?.copy() as RestCallAction?
    }

    private fun updateParameterGenesWithRequest(parameter: Param, postmanRequest: Request, restAction: RestCallAction) {
        if (!isFormBody(parameter)) { // Form bodies in Postman are not a single string but an array of key-value
            val paramValue = getParamValueFromRequest(parameter, postmanRequest, restAction)
            updateGenesRecursivelyWithParameterValue(parameter.gene, parameter.name, paramValue)
        } else {

        }

    }

    /**
     * In a Postman collection file, all parameter values are represented as strings,
     * except for form bodies.
     *
     * @param parameter Parameter extracted from an action
     * @param postmanRequest Postman representation of a request
     * @param restAction Action where the parameter is contained. Needed to find
     * path parameters, since Postman doesn't use keys for them, only values
     * @return Value of the parameter in the Postman request, null if not found
     */
    private fun getParamValueFromRequest(parameter: Param, postmanRequest: Request, restAction: RestCallAction): String? {
        var value: String? = null
        when (parameter) {
            is HeaderParam -> value = postmanRequest.header?.find { it.key == parameter.name }?.value
            is QueryParam -> value = postmanRequest.url.query?.find { it.key == parameter.name }?.value
            is BodyParam -> value = postmanRequest.body?.raw // Will return null for form bodies
            is PathParam -> {
                val path = URI(postmanRequest.url.raw).path.trim()
                value = restAction.path.getKeyValues(path)?.get(parameter.name)
            }
        }

        return value
    }

    private fun isFormBody(parameter: Param): Boolean {
        return parameter is BodyParam && parameter.isForm()
    }

    private fun getChildrenGenes(parentGene: Gene): List<Gene> {
        return parentGene.flatView { it.parent == parentGene }
    }
}