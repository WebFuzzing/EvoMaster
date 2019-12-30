package org.evomaster.core.problem.rest

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
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
import java.util.concurrent.atomic.AtomicInteger

/**
 * https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md
 *
 *
 */
object RestActionBuilderV3 {

    private val log: Logger = LoggerFactory.getLogger(RestActionBuilderV3::class.java)
    private val idGenerator = AtomicInteger()


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

                    if(!e.value.description.isNullOrBlank()){
                        //TODO should we do something with it for doParseDescription?
                    }

                    if (e.value.get != null) handleOperation(actionCluster, HttpVerb.GET, restPath, e.value.get,doParseDescription)
                    if (e.value.post != null) handleOperation(actionCluster, HttpVerb.POST, restPath, e.value.post,doParseDescription)
                    if (e.value.put != null) handleOperation(actionCluster, HttpVerb.PUT, restPath, e.value.put,doParseDescription)
                    if (e.value.patch != null) handleOperation(actionCluster, HttpVerb.PATCH, restPath, e.value.patch,doParseDescription)
                    if (e.value.options != null) handleOperation(actionCluster, HttpVerb.OPTIONS, restPath, e.value.options,doParseDescription)
                    if (e.value.delete != null) handleOperation(actionCluster, HttpVerb.DELETE, restPath, e.value.delete,doParseDescription)
                    if (e.value.trace != null) handleOperation(actionCluster, HttpVerb.TRACE, restPath, e.value.trace,doParseDescription)
                    if (e.value.head != null) handleOperation(actionCluster, HttpVerb.HEAD, restPath, e.value.head,doParseDescription)
                }

        checkSkipped(skipped, endpointsToSkip, actionCluster)
    }

    private fun handleOperation(
            actionCluster: MutableMap<String, Action>,
            verb: HttpVerb,
            restPath: RestPath,
            operation: Operation,
            doParseDescription: Boolean) {

        val params = extractParams(restPath, operation)

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
            operation: Operation
    ): MutableList<Param> {

        val params = mutableListOf<Param>()

        removeDuplicatedParams(operation.parameters)
                .forEach { p ->

                    val name = p.name ?: "undefined"

                    var gene = getGene(name, p.schema)

                    if (p.`in` == "path" && gene is StringGene) {
                        /*
                            We want to avoid empty paths, and special chars like / which
                            would lead to 2 variables, or any other char that does affect the
                            structure of the URL, like '.'
                         */
                        gene = StringGene(gene.name, (gene as StringGene).value, 1, (gene as StringGene).maxLength, listOf('/', '.'))
                    }

                    if (p.required != true && p.`in` != "path") {
                        // As of V3, "path" parameters must be required
                        gene = OptionalGene(name, gene)
                    }

                    //TODO could exploit "x-example" if available in Swagger

                    when (p.`in`) {
                        "query" -> params.add(QueryParam(name, gene))
                        "path" -> params.add(PathParam(name, DisruptiveGene("d_", gene, 1.0)))
                        "header" -> params.add(HeaderParam(name, gene))
                        //TODO "cookie"
                        else -> throw IllegalStateException("Unrecognized: ${p.getIn()}")
                    }
                }

        //TODO do we need repairParams?

        if(operation.requestBody != null){

            val body = operation.requestBody!!

            val name = "body"

            if(body.content.isEmpty()){
                log.warn("No 'content' field in body payload for: $restPath")
            } else {

                /*
                    FIXME as of V3, different types might have different body definitions.
                    This should refactored to enable possibility of different BodyParams
                */

                val obj: MediaType = body.content.values.first()

                var gene = fromMediaType(obj)

                if (body.required != true) {
                    gene = OptionalGene(name, gene)
                }

                val contentTypeGene = EnumGene<String>("contentType", body.content.keys)

                params.add(BodyParam(gene, contentTypeGene))
            }
        }


        return params
    }

    private fun fromMediaType(obj: MediaType): Gene {

        //TODO
        return IntegerGene("TODO")
    }

    private fun getGene(
            name: String,
            schema: Schema<Any>,
            history: MutableList<String> = mutableListOf()
    ): Gene {

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

        if (type == "string" && schema.enum?.isNotEmpty() == true) {
            //TODO enum can be for any type, not just strings
            //Besides the defined values, add one to test robustness
            return EnumGene(name, (schema.enum as MutableList<String>).apply { add("EVOMASTER") })
        }

        //TODO enum integer

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
                return if(schema.pattern == null){
                    StringGene(name)
                } else {
                    try {
                        RegexHandler.createGeneForEcma262(schema.pattern)
                    } catch (e: Exception){
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
//            "ref" -> {
//                if (property == null) {
//                    //TODO somehow will need to handle it
//                    throw IllegalStateException("Cannot handle ref out of a property")
//                }
//                val rp = property as RefProperty
//                return RestActionBuilder.createObjectFromReference(name, rp.`$ref`, swagger, history)
//            }
//            "array" -> {
//
//                val items = when {
//                    property != null -> (property as ArrayProperty).items
//                    parameter != null -> parameter.getItems()
//                    else -> throw IllegalStateException("Failed to handle array")
//                }
//
//                val template = RestActionBuilder.getGene(
//                        name + "_item",
//                        items.type,
//                        items.format,
//                        null, // no pattern available?
//                        swagger,
//                        items,
//                        null,
//                        history)
//
//                if (template is CycleObjectGene) {
//                    return CycleObjectGene("<array> ${template.name}")
//                }
//
//                return ArrayGene(name, template)
//            }
//            "object" -> {
//                if (property == null) {
//                    //TODO somehow will need to handle it
//                    throw IllegalStateException("Cannot handle object out of a property")
//                }
//
//                if (property is MapProperty) {
//                    val ap = property.additionalProperties
//                    return RestActionBuilder.createMapGene(
//                            name, // + "_map", BMR: here's hoping nothing crashes + "_map",
//                            ap.type,
//                            ap.format,
//                            swagger,
//                            ap,
//                            history)
//                }
//
//
//                if (property is ObjectProperty) {
//
//                    val fields = RestActionBuilder.createFields(property.properties, swagger, history)
//                    return ObjectGene(name, fields, property.type)
//                }
//            }
            "file" -> return StringGene(name) //TODO file is a hack. I want to find a more elegant way of dealing with it (BMR)
        }

        throw IllegalArgumentException("Cannot handle combination $type/$format")
    }

    private fun removeDuplicatedParams(parameters: List<Parameter>?): List<Parameter> {

        /*
            Duplicates are not allowed.
            TODO should issue a warning if it happens
            TODO detect duplicates... but would parser even allow it???
            https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#operationObject
            combination of "name" and "location"
         */

        return parameters ?: listOf()
    }

//    private fun extractParams(
//            opEntry: Map.Entry<HttpMethod, Operation>,
//            swagger: Swagger,
//            restPath: RestPath
//    ): MutableList<Param> {
//
//        val params: MutableList<Param> = mutableListOf()
//        val operation = opEntry.value
//
//        RestActionBuilder.removeDuplicatedParams(operation.parameters)
//                .forEach { p ->
//
//                    val name = p.name ?: "undefined"
//
//                    if (p is AbstractSerializableParameter<*>) {
//
//                        val type = p.getType() ?: run {
//                            RestActionBuilder.log.warn("Missing/invalid type for '$name' in Swagger file. Using default 'string'")
//                            "string"
//                        }
//
//                        var gene = RestActionBuilder.getGene(name, type, p.getFormat(), p.getPattern(), swagger, null, p)
//
//                        if(p.`in` == "path" && gene is StringGene){
//                            /*
//                                We want to avoid empty paths, and special chars like / which
//                                would lead to 2 variables, or anyh other char that does affect the
//                                structure of the URL, like '.'
//                             */
//                            gene = StringGene(gene.name, gene.value, 1, gene.maxLength, listOf('/', '.'))
//                        }
//
//                        if (!p.required && p.`in` != "path") {
//                            /*
//                                Even if a "path" parameter might not be required, still
//                                do not use an optional for it. Otherwise, might
//                                end up in quite a few useless 405 errors.
//
//                                Furthermore, "path" parameters must be "required" according
//                                to specs.
//                                TODO: could issue warning that Swagger is incorrect
//                            */
//                            gene = OptionalGene(name, gene)
//                        }
//
//                        //TODO could exploit "x-example" if available in Swagger
//
//                        when (p.`in`) {
//                            "query" -> params.add(QueryParam(name, gene))
//                            "path" -> params.add(PathParam(name, DisruptiveGene("d_", gene, 1.0)))
//                            "header" -> params.add(HeaderParam(name, gene))
//                            "formData" -> params.add(FormParam(name, gene))
//                            else -> throw IllegalStateException("Unrecognized: ${p.getIn()}")
//                        }
//
//                    } else if (p is BodyParameter
//                            && !RestActionBuilder.shouldAvoidCreatingObject(p, swagger)
//                            && opEntry.key != HttpMethod.GET
//                    ) {
//
//                        val name = "body"
//
//                        var gene = p.schema.reference?.let { RestActionBuilder.createObjectFromReference(name, it, swagger) }
//                                ?: (p.schema as ModelImpl).let {
//                                    if (it.type == "object") {
//                                        RestActionBuilder.createObjectFromModel(p.schema, "body", swagger, it.type)
//                                    } else {
//                                        RestActionBuilder.getGene(name, it.type, it.format, it.pattern, swagger)
//                                    }
//                                }
//
//                        if (!p.required) {
//                            gene = OptionalGene(name, gene)
//                        }
//
//                        var types = operation.consumes
//                        if (types == null || types.isEmpty()) {
//                            val msg = "Missing consume types in body payload definition. Defaulting to JSON. Endpoint: ${opEntry.key} $restPath}"
//                            RestActionBuilder.log.warn(msg)
//                            types = listOf("application/json")
//                        }
//
//                        val contentTypeGene = EnumGene<String>("contentType", types)
//
//                        params.add(BodyParam(gene, contentTypeGene))
//                    }
//                }
//
//        return params
//    }


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