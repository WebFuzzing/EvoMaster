package org.evomaster.core.problem.rest.seeding.postman

import com.google.gson.Gson
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.problem.rest.seeding.AbstractParser
import org.evomaster.core.problem.rest.seeding.postman.pojos.PostmanCollectionObject
import org.evomaster.core.problem.rest.seeding.postman.pojos.Request
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.IllegalStateException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Deprecated("Code here will be replaced with intermediate representation. See new 'seeding' package")
class PostmanParser(
        defaultRestCallActions: List<RestCallAction>,
        swagger: OpenAPI
) : AbstractParser(defaultRestCallActions, swagger) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PostmanParser::class.java)
        private const val TMP_PATH_STR = "_TEMP_REPLACE_1234_ABCD_"
    }

    override fun parseTestCases(path: String): MutableList<MutableList<RestCallAction>> {
        log.info("Parsing seed test cases from Postman collection located at {}", path)

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
        val path = getPath(postmanRequest.url.path)
        val originalRestAction = defaultRestActions.firstOrNull { it.verb.toString() == verb && it.path.matches(path) }

        if (originalRestAction == null)
            log.warn("Endpoint {} not found in the Swagger", "$verb:$path")

        return originalRestAction?.copy() as RestCallAction?
    }

    private fun updateParameterGenesWithRequest(parameter: Param, postmanRequest: Request, restAction: RestCallAction) {
        if (!isFormBody(parameter)) { // Form bodies in Postman are not a single string but an array of key-value
            val paramValue = getParamValueFromRequest(parameter, postmanRequest, restAction)
            updateGenesRecursivelyWithParameterValue(parameter.gene, parameter.name, paramValue)
        } else
            updateFormBodyGenesWithRequest(parameter as BodyParam, postmanRequest)
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
            is QueryParam -> {
                value = postmanRequest.url.query?.find { it.key == parameter.name }?.value
                if (value != null)
                    value = URLDecoder.decode(value, StandardCharsets.UTF_8.toString()) // Query params must be encoded
            }
            is BodyParam -> value = postmanRequest.body?.raw // Will return null for form bodies
            is PathParam -> {
                val path = getPath(postmanRequest.url.path)
                val pathParamValue = restAction.path.getKeyValues(path)?.get(parameter.name)
                if (pathParamValue == null)
                    log.warn("Ignoring path parameter value... RestAction path and Postman path do not match: {} vs {}", restAction.path.toString(), path)
                else
                    value = getDecodedPathElement(pathParamValue)
            }
        }

        return value
    }

    /**
     * Form bodies in Postman are not a single raw string, but rather a list of
     * key-value pairs. This function takes a form body parameter containing an
     * object gene and updates its first-level subgenes according to the key-value
     * pairs present in the Postman request.
     */
    private fun updateFormBodyGenesWithRequest(formBody: BodyParam, postmanRequest: Request) {
        /*
            TODO: Support nested objects according to different serialization schemes [1].
             The payload format is similar to that of query parameters, therefore it
             would be possible to parse a Postman-formatted form body according to the
             serialization scheme defined in the Swagger [2].
             [1] https://swagger.io/docs/specification/describing-request-body/ (section "Form Data")
             [2] https://swagger.io/docs/specification/describing-parameters/#query-parameters
         */

        // Optional form body not present in request
        if (postmanRequest.body?.urlencoded?.isEmpty() != false) {
            if (formBody.gene is OptionalGene)
                formBody.gene.isActive = false
            else
                log.warn("Required form body was not found in a seeded request. Ignoring and keeping the body...")
            return
        }

        when (val rootGene = if (formBody.gene is OptionalGene) formBody.gene.gene else formBody.gene) {
            is ObjectGene -> {
                rootGene.fields.forEach { formBodyField ->
                    val paramValue = postmanRequest.body.urlencoded.find { it.key == formBodyField.name }?.value
                    updateGenesRecursivelyWithParameterValue(formBodyField, formBodyField.name, paramValue)
                }
            }

            else -> throw IllegalStateException("Only objects are supported for form bodies when parsing Postman requests")
        }
    }

    private fun getPath(pathElements: List<String>): String {
        return "/" + pathElements.joinToString("/")
    }

    /**
     * Path elements are encoded/decoded differently that query elements in a URL.
     * Actually, the only problem when decoding path elements are white spaces,
     * which could be encoded as "+" in the query. When decoding a "+" in the path,
     * it should remain as "+" and not be changed for " ".
     *
     * This method decodes path elements using the standard URLDecoder class from
     * the Java API, but replaces "+" chars with a temporary string before and
     * after, so that those do not get transformed to " ".
     */
    private fun getDecodedPathElement(pathElement: String): String {
        return URLDecoder.decode(
                pathElement.replace("+", TMP_PATH_STR),
                StandardCharsets.UTF_8.toString()
        ).replace(TMP_PATH_STR, "+")
    }

    private fun isFormBody(parameter: Param): Boolean {
        return parameter is BodyParam && parameter.isForm()
    }
}