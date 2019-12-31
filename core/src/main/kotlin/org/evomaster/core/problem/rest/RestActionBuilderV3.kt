package org.evomaster.core.problem.rest

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.atomic.AtomicInteger

/**
 * https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md
 *
 *  Create actions from a OpenApi/Swagger schema.
 *  Must support both V2 and V3 specs.
 *
 */
object RestActionBuilderV3 {

    private val log: Logger = LoggerFactory.getLogger(RestActionBuilderV3::class.java)
    private val idGenerator = AtomicInteger()

    private val refCache = mutableMapOf<String, Gene>()


    /**
     * @param doParseDescription presents whether apply name/text analysis on description and summary of rest action
     */
    fun addActionsFromSwagger(swagger: OpenAPI,
                              actionCluster: MutableMap<String, Action>,
                              endpointsToSkip: List<String> = listOf(),
                              doParseDescription: Boolean = false) {

        actionCluster.clear()

        val skipped = mutableListOf<String>()

        swagger.paths
                .filter { e ->
                    if (endpointsToSkip.contains(e.key)) {
                        skipped.add(e.key)
                        false
                    } else {
                        true
                    }
                }
                .forEach { e ->

                    /*
                        In V2 there that a "host" and "basePath".
                        In V3, this was replaced by a "servers" list of URLs.
                        The "paths" are then appended to such URLs, which works
                        like a "host+basePath"
                     */

                    val restPath = RestPath(e.key)

                    if (e.value.`$ref` != null) {
                        //TODO
                        log.warn("Currently cannot handle \$ref: ${e.value.`$ref`}")
                    }

                    if (e.value.parameters != null && e.value.parameters.isNotEmpty()) {
                        //TODO
                        log.warn("Currently cannot handle 'path-scope' parameters")
                    }

                    if (!e.value.description.isNullOrBlank()) {
                        //TODO should we do something with it for doParseDescription?
                    }

                    if (e.value.get != null) handleOperation(actionCluster, HttpVerb.GET, restPath, e.value.get, swagger, doParseDescription)
                    if (e.value.post != null) handleOperation(actionCluster, HttpVerb.POST, restPath, e.value.post, swagger, doParseDescription)
                    if (e.value.put != null) handleOperation(actionCluster, HttpVerb.PUT, restPath, e.value.put, swagger, doParseDescription)
                    if (e.value.patch != null) handleOperation(actionCluster, HttpVerb.PATCH, restPath, e.value.patch, swagger, doParseDescription)
                    if (e.value.options != null) handleOperation(actionCluster, HttpVerb.OPTIONS, restPath, e.value.options, swagger, doParseDescription)
                    if (e.value.delete != null) handleOperation(actionCluster, HttpVerb.DELETE, restPath, e.value.delete, swagger, doParseDescription)
                    if (e.value.trace != null) handleOperation(actionCluster, HttpVerb.TRACE, restPath, e.value.trace, swagger, doParseDescription)
                    if (e.value.head != null) handleOperation(actionCluster, HttpVerb.HEAD, restPath, e.value.head, swagger, doParseDescription)
                }

        checkSkipped(skipped, endpointsToSkip, actionCluster)
    }

    private fun handleOperation(
            actionCluster: MutableMap<String, Action>,
            verb: HttpVerb,
            restPath: RestPath,
            operation: Operation,
            swagger: OpenAPI,
            doParseDescription: Boolean) {

        val params = extractParams(restPath, operation, swagger)

        val produces = operation.responses?.values //different response objects based on HTTP code
                ?.filter { it.content != null && it.content.isNotEmpty() }
                //each response can have different media-types
                ?.flatMap { it.content.keys }
                ?.toSet() // remove duplicates
                ?.toList()
                ?: listOf()

        val actionId = "$verb$restPath${idGenerator.incrementAndGet()}"
        val action = RestCallAction(actionId, verb, restPath, params, produces = produces)

        //TODO update for new parser
//                        /*This section collects information regarding the types of data that are
//                        used in the response of an action (if such data references are provided in the
//                        swagger definition
//                        */
//                        val responses = o.value.responses.filter { it.value.responseSchema != null }
//
//                        if (responses.isNotEmpty()) {
//                            responses.filter { it.value.responseSchema is RefModel }.forEach { (k, v) ->
//                                action.addRef(k, (v.responseSchema as RefModel).simpleRef)
//                            }
//                        }

        if (doParseDescription) {
            var info = operation.description
            if (!info.isNullOrBlank() && !info.endsWith(".")) info += "."
            if (!operation.summary.isNullOrBlank()) info = if (info == null) operation.summary else (info + " " + operation.summary)
            if (!info.isNullOrBlank() && !info.endsWith(".")) info += "."
            action.initTokens(info)
        }

        actionCluster[action.getName()] = action
    }


    private fun extractParams(
            restPath: RestPath,
            operation: Operation,
            swagger: OpenAPI
    ): MutableList<Param> {

        val params = mutableListOf<Param>()

        removeDuplicatedParams(operation)
                .forEach { p ->

                    val name = p.name ?: "undefined"

                    var gene = getGene(name, p.schema, swagger)

                    if (p.`in` == "path" && gene is StringGene) {
                        /*
                            We want to avoid empty paths, and special chars like / which
                            would lead to 2 variables, or any other char that does affect the
                            structure of the URL, like '.'
                         */
                        gene = StringGene(gene.name, (gene as StringGene).value, 1, (gene as StringGene).maxLength, listOf('/', '.'))
                    }

                    if (p.required != true && p.`in` != "path" && gene !is OptionalGene) {
                        // As of V3, "path" parameters must be required
                        gene = OptionalGene(name, gene)
                    }

                    //TODO could exploit "x-example" if available in OpenApi

                    when (p.`in`) {
                        "query" -> params.add(QueryParam(name, gene))
                        "path" -> params.add(PathParam(name, DisruptiveGene("d_", gene, 1.0)))
                        "header" -> params.add(HeaderParam(name, gene))
                        //TODO "cookie"
                        else -> throw IllegalStateException("Unrecognized: ${p.getIn()}")
                    }
                }

        //TODO do we need repairParams?

        if (operation.requestBody != null) {

            val body = operation.requestBody!!

            val name = "body"

            if (body.content.isEmpty()) {
                log.warn("No 'content' field in body payload for: $restPath")
            } else {

                /*
                    FIXME as of V3, different types might have different body definitions.
                    This should refactored to enable possibility of different BodyParams
                */

                val obj: MediaType = body.content.values.first()

                var gene = getGene("body", obj.schema, swagger)

                if (body.required != true && gene !is OptionalGene) {
                    gene = OptionalGene(name, gene)
                }

                val contentTypeGene = EnumGene<String>("contentType", body.content.keys)

                params.add(BodyParam(gene, contentTypeGene))
            }
        }


        return params
    }

    private fun possiblyOptional(gene: Gene, required: Boolean?): Gene {

        if (required != true) {
            return OptionalGene(gene.name, gene)
        }

        return gene
    }

    private fun getGene(
            name: String,
            schema: Schema<*>,
            swagger: OpenAPI,
            history: MutableList<String> = mutableListOf()
    ): Gene {

        if (!schema.`$ref`.isNullOrBlank()) {
            return createObjectFromReference(name, schema.`$ref`, swagger, history)
        }


        /*
            https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#dataTypeFormat

        Common Name	    type	format	Comments
        integer	        integer	int32	signed 32 bits
        long	        integer	int64	signed 64 bits
        float	        number	float
        double	        number	double
        string	        string
        byte	        string	byte	base64 encoded characters
        binary	        string	binary	any sequence of octets
        boolean	        boolean
        date	        string	date	As defined by full-date - RFC3339
        dateTime	    string	date-time	As defined by date-time - RFC3339
        password	    string	password	Used to hint UIs the input needs to be obscured.
         */

        val type = schema.type
        val format = schema.format

        if (schema.enum?.isNotEmpty() == true) {

            //Besides the defined values, add one to test robustness
            when(type){
                "string" ->
                    return EnumGene(name, (schema.enum as MutableList<String>).apply { add("EVOMASTER") })
                "number", "integer" -> {
                    if (format == "double" || format == "float") {
                        return EnumGene(name, (schema.enum as MutableList<Double>).apply { add(42.0) })
                    }
                    return EnumGene(name, (schema.enum as MutableList<Long>).apply { add(42) })
                }
                else -> log.warn("Cannot handle enum of type: $type")
            }
        }

        //first check for "optional" format
        when (format) {
            "int32" -> return IntegerGene(name)
            "int64" -> return LongGene(name)
            "double" -> return DoubleGene(name)
            "float" -> return FloatGene(name)
            "password" -> return StringGene(name) //nothing special to do, it is just a hint
            "binary" -> return StringGene(name) //does it need to be treated specially?
            "byte" -> return Base64StringGene(name)
            "date" -> return DateGene(name)
            "date-time" -> return DateTimeGene(name)
            else -> if (format != null) {
                log.warn("Unhandled format '$format'")
            }
        }

        /*
                If a format is not defined, the type should default to
                the JSON Schema definition
            */
        when (type) {
            "integer" -> return IntegerGene(name)
            "number" -> return DoubleGene(name)
            "boolean" -> return BooleanGene(name)
            "string" -> {
                return if (schema.pattern == null) {
                    StringGene(name)
                } else {
                    try {
                        RegexHandler.createGeneForEcma262(schema.pattern)
                    } catch (e: Exception) {
                        /*
                            TODO: if the Regex is syntactically invalid, we should warn
                            the user. But, as we do not support 100% regex, might be an issue
                            with EvoMaster. Anyway, in such cases, instead of crashing EM, let's just
                            take it as a String.
                            When 100% support, then tell user that it is his/her fault
                         */
                        LoggingUtil.uniqueWarn(log, "Cannot handle regex: ${schema.pattern}")
                        StringGene(name)
                    }
                }
            }
            "array" -> {
                if (schema is ArraySchema) {
                    val template = getGene(name + "_item", schema.items, swagger, history)

                    if (template is CycleObjectGene) {
                        return CycleObjectGene("<array> ${template.name}")
                    }
                    return ArrayGene(name, template)
                } else {
                    log.warn("Invalid 'array' definition for '$name'")
                }
            }

            "object" -> {
                return createObjectGene(name, schema, swagger, history)
            }

            "file" -> return StringGene(name) //TODO file is a hack. I want to find a more elegant way of dealing with it (BMR)
        }

        if(name == "body" && schema.properties?.isNotEmpty() == true) {
            /*
                This could happen when parsing a body-payload as formData
            */
            return createObjectGene(name, schema, swagger, history)
        }


        throw IllegalArgumentException("Cannot handle combination $type/$format")
    }

    private fun createObjectGene(name: String, schema: Schema<*>, swagger: OpenAPI, history: MutableList<String>): Gene {

        val fields = schema.properties?.entries?.map {
            possiblyOptional(
                    getGene(it.key, it.value, swagger, history),
                    schema.required?.contains(it.key)
            )
        } ?: listOf()

        /*
                    Can be either a boolean or a Schema
                 */
        val additional = schema.additionalProperties

        if (additional is Boolean) {
            /*
                        if 'false', no other fields besides the specified ones can be added.
                        Default is 'true'.
                     */
            //TODO could add extra fields for robustness testing
        }
        if (additional is Schema<*>) {
            /*
                        TODO could add extra fields for robustness testing,
                        with and without following the given schema for their type
                     */
        }

        //TODO allOf, anyOf, oneOf and not

        if (fields.isEmpty()) {
            log.warn("No fields for object definition: $name")
            return MapGene(name, StringGene(name + "_field"))
        }

        return ObjectGene(name, fields)
    }


    private fun createObjectFromReference(name: String,
                                          reference: String,
                                          swagger: OpenAPI,
                                          history: MutableList<String> = mutableListOf()
    ): Gene {

        /*
            We need to avoid cycles like A.B.A...
         */
        if (history.contains(reference)) {
            return CycleObjectGene("Cycle for: $reference")
        }
        history.add(reference)


        if(refCache.containsKey(reference)){
            return refCache[reference]!!.copy()
        }

        try {
            URI(reference)
        } catch (e: URISyntaxException) {
            LoggingUtil.uniqueWarn(log, "Object reference is not a valid URI: $reference")
        }

        //token after last /
        val classDef = reference.substring(reference.lastIndexOf("/") + 1)

        val schema = swagger.components.schemas[classDef]
        if (schema == null) {
            log.warn("No $classDef among the object definitions in the OpenApi file")
            return ObjectGene(name, listOf(), classDef)
        }

        val gene = getGene(name, schema, swagger, history)
        refCache[reference] = gene
        return gene
    }


    private fun removeDuplicatedParams(operation: Operation): List<Parameter> {

        /*
            Duplicates are not allowed, based on combination of "name" and "location".
            https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#operationObject
         */

        if(operation.parameters == null){
            return listOf()
        }

        val selection = mutableListOf<Parameter>()
        val seen = mutableSetOf<String>()

        for(p in operation.parameters){

            val key = p.`in` + "_" + p.name
            if(! seen.contains(key)){
                seen.add(key)
                selection.add(p)
            }
        }

        val diff = operation.parameters.size - selection.size
        if(diff > 0){
            log.warn("Operation ${operation.operationId} has $diff repeated parameters")
        }

        return selection
    }


    private fun checkSkipped(skipped: MutableList<String>, endpointsToSkip: List<String>, actionCluster: MutableMap<String, Action>) {
        if (skipped.size != endpointsToSkip.size) {
            val msg = "${endpointsToSkip.size} were set to be skipped, but only ${skipped.size}" +
                    " were found in the schema"
            LoggingUtil.getInfoLogger().error(msg)
            endpointsToSkip.filter { !skipped.contains(it) }
                    .forEach { LoggingUtil.getInfoLogger().warn("Missing endpoint: $it") }
            throw SutProblemException(msg)
        }

        LoggingUtil.getInfoLogger().apply {
            if (skipped.size != 0) {
                info("Skipped ${skipped.size} path endpoints from the schema configuration")
            }

            val n = actionCluster.size
            when (n) {
                0 -> warn("There is _NO_ usable RESTful API endpoint defined in the schema configuration")
                1 -> info("There is only one usable RESTful API endpoint defined in the schema configuration")
                else -> info("There are $n usable RESTful API endpoints defined in the schema configuration")
            }
        }
    }

}