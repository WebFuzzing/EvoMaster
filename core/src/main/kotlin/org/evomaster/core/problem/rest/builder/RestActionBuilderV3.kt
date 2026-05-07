package org.evomaster.core.problem.rest.builder

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.EMConfig
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import org.evomaster.client.java.instrumentation.shared.ClassToSchemaUtils.OPENAPI_COMPONENT_NAME
import org.evomaster.client.java.instrumentation.shared.ClassToSchemaUtils.OPENAPI_SCHEMA_NAME
import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.problem.rest.link.RestLink
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaOpenAPI
import org.evomaster.core.problem.rest.schema.SchemaUtils
import org.evomaster.core.problem.util.ActionBuilderUtil
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.builder.JsonSchemaToGeneConverter
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.CustomMutationRateGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayDeque

/**
 * https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md
 *
 *  Create actions from a OpenApi/Swagger schema.
 *  Must support both V2 and V3 specs.
 *
 */
object RestActionBuilderV3 {

    private val log: Logger = LoggerFactory.getLogger(RestActionBuilderV3::class.java)

    /**
     * Name given to enum genes representing data examples coming from OpenAPI schema
     */
    const val EXAMPLES_NAME = "SCHEMA_EXAMPLES"

    /**
     * Key -> schema in the form "name: {...}"
     * Value -> object gene for it
     */
    private val dtoCache = mutableMapOf<String, Gene>()

    private val mapper = ObjectMapper()

    class Options(
        /**
         * presents whether apply name/text analysis on description and summary of rest action
         */
        @Deprecated("No longer maintained")
        val doParseDescription: Boolean = false,
        /**
         * Whether constraints should be considered and satisfied, eg min/max for numbers
         */
        val enableConstraintHandling: Boolean = true,
        /**
         * Purposely add invalid data, eg wrongly formatted dates.
         * TODO remove/deprecate once we support Robustness Testing
         */
        val invalidData: Boolean = false,

        val probUseDefault: Double = 0.0,

        val probUseExamples: Double = 0.0,

        /**
        If we are doing white-box testing, we might use advance techniques like taint analysis,
        which might impact how we design the chromosome.
        but, for black-box, they would not be useful
         */
        val usingWhiteBox: Boolean = true
    ){
        constructor(config: EMConfig): this(
            enableConstraintHandling = config.enableSchemaConstraintHandling,
            invalidData = config.allowInvalidData,
            probUseDefault = config.probRestDefault,
            probUseExamples = config.probRestExamples,
            usingWhiteBox = !config.blackBox
        )

        init {
            if(probUseDefault < 0 || probUseDefault > 1){
                throw IllegalArgumentException("Invalid probUseDefault: $probUseDefault")
            }
            if(probUseExamples < 0 || probUseExamples > 1){
                throw IllegalArgumentException("Invalid probUseExamples: $probUseExamples")
            }
        }
    }


    /**
     * clean cache in order to avoid different dto schema with different configurations, eg, enableConstraintHandling.
     */
    fun cleanCache(){
        dtoCache.clear()
    }

    /**
     * Build a [JsonSchemaToGeneConverter] backed by [schemaHolder].
     * The converter owns its own `$ref` cache, so callers should keep the
     * returned instance alive for the duration of a single build to benefit
     * from gene reuse across operations.
     */
    private fun newConverter(schemaHolder: RestSchema, options: Options): JsonSchemaToGeneConverter {
        return JsonSchemaToGeneConverter(
            RestSchemaRefResolver(schemaHolder, schemaHolder.main),
            options.toConverterOptions()
        )
    }

    private fun newConverter(
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        options: Options
    ): JsonSchemaToGeneConverter {
        return JsonSchemaToGeneConverter(
            RestSchemaRefResolver(schemaHolder, currentSchema),
            options.toConverterOptions()
        )
    }

    private fun Options.toConverterOptions(): JsonSchemaToGeneConverter.Options {
        return JsonSchemaToGeneConverter.Options(
            doParseDescription = this.doParseDescription,
            enableConstraintHandling = this.enableConstraintHandling,
            invalidData = this.invalidData,
            probUseDefault = this.probUseDefault,
            probUseExamples = this.probUseExamples,
            usingWhiteBox = this.usingWhiteBox
        )
    }


    @Deprecated("User other version instead")
    fun addActionsFromSwagger(swagger: OpenAPI,
                              actionCluster: MutableMap<String, Action>,
                              endpointsToSkip: List<Endpoint> = listOf(),
                              doParseDescription: Boolean = false,
                              enableConstraintHandling: Boolean
    ) : List<String> {
        return addActionsFromSwagger(RestSchema(SchemaOpenAPI("",swagger, SchemaLocation.MEMORY)), actionCluster, endpointsToSkip,
            Options(doParseDescription = doParseDescription, enableConstraintHandling = enableConstraintHandling)
        )
    }

    /**
     * Given a parsed OpenAPI schema, create action templates that will be added to the input [actionCluster]/
     *
     * @return list of error/warning messages
     */
    fun addActionsFromSwagger(schemaHolder: RestSchema,
                              actionCluster: MutableMap<String, Action>,
                              endpointsToSkip: List<Endpoint> = listOf(),
                              options: Options
    ) : List<String> {

        actionCluster.clear()
        cleanCache()

        val messages = mutableListOf<String>()
        val skipped = mutableListOf<Endpoint>()
        val errorEndpoints = mutableListOf<String>()

        val swagger = schemaHolder.main.schemaParsed
        val converter = newConverter(schemaHolder, options)

        swagger.paths
            .forEach { e ->
                handlePathItem(
                    e.key,
                    e.value,
                    messages,
                    endpointsToSkip,
                    skipped,
                    actionCluster,
                    schemaHolder,
                    converter,
                    options,
                    errorEndpoints
                )
            }

        ActionBuilderUtil.verifySkipped(skipped,endpointsToSkip)
        ActionBuilderUtil.printActionNumberInfo("RESTful API", actionCluster.size, skipped.size, errorEndpoints.size)

        return messages
    }

    private fun handlePathItem(
        pathKey: String,
        pathItem: PathItem,
        messages: MutableList<String>,
        endpointsToSkip: List<Endpoint>,
        skipped: MutableList<Endpoint>,
        actionCluster: MutableMap<String, Action>,
        schemaHolder: RestSchema,
        converter: JsonSchemaToGeneConverter,
        options: Options,
        errorEndpoints: MutableList<String>
    ) {
        val swagger = schemaHolder.main.schemaParsed
        val basePath = getBasePathFromURL(swagger)

        /*
           In V2 there that a "host" and "basePath".
           In V3, this was replaced by a "servers" list of URLs.
           The "paths" are then appended to such URLs, which works
           like a "host+basePath"
         */
        val restPath = RestPath(if (basePath == "/") pathKey else (basePath + pathKey))

        //filtering is based on raw path declaration, without base
        //TODO might want to support both cases in the future
        val rawPath = RestPath(pathKey)

        if (pathItem.`$ref` != null) {
            val pi = SchemaUtils.getReferencePathItem(schemaHolder,schemaHolder.main,pathItem.`$ref`,messages)
                ?: return
            handlePathItem(pathKey, pi, messages,endpointsToSkip,skipped,actionCluster,schemaHolder,converter,options,errorEndpoints)
            return
        }

        if (pathItem.parameters != null && pathItem.parameters.isNotEmpty()) {
            /*
              TODO this is for parameters that apply to all endpoints for given path
             */
            messages.add("Currently cannot handle 'path-scope' parameters in $pathKey")
        }

        if (!pathItem.description.isNullOrBlank()) {
            //TODO should we do something with it for doParseDescription?
        }

        val h = { verb: HttpVerb, operation: Operation ->
            if (endpointsToSkip.any { it.verb == verb && it.path.isEquivalent(rawPath) }) {
                skipped.add(Endpoint(verb, restPath))
            } else {
                handleOperation(
                    actionCluster,
                    verb,
                    restPath,
                    operation,
                    schemaHolder,
                    schemaHolder.main,
                    converter,
                    options,
                    errorEndpoints,
                    messages
                )
            }
        }

        if (pathItem.get != null) h(HttpVerb.GET, pathItem.get)
        if (pathItem.post != null) h(HttpVerb.POST, pathItem.post)
        if (pathItem.put != null) h(HttpVerb.PUT, pathItem.put)
        if (pathItem.patch != null) h(HttpVerb.PATCH, pathItem.patch)
        if (pathItem.options != null) h(HttpVerb.OPTIONS, pathItem.options)
        if (pathItem.delete != null) h(HttpVerb.DELETE, pathItem.delete)
        if (pathItem.trace != null) h(HttpVerb.TRACE, pathItem.trace)
        if (pathItem.head != null) h(HttpVerb.HEAD, pathItem.head)
    }

    /**
     * Creates a Gene object for Data Transfer Objects (DTOs) based on the provided parameters.
     *
     * @param dtoSchemaName The name of the DTO schema to create a Gene for.
     * @param allSchemas The string containing all DTO schemas, including the one specified by 'dtoSchemaName'.
     *      The expected format is "name: { "type name": "schema", "ref name": "schema", .... }"
     * @param options The options to customize the gene creation process.
     * @return A Gene object representing the specified DTO schema.
     * @throws IllegalArgumentException if the provided 'name' is not found at the beginning of 'allSchemas'.
     * @throws IllegalStateException if the schema with the specified 'name' cannot be found in 'allSchemas'.
     *
     * @see Gene
     * @see Options
     */
    fun createGeneForDTO(dtoSchemaName: String,
                         allSchemas: String,
                         options: Options
    ) : Gene{
        if(!allSchemas.startsWith("\"$dtoSchemaName\"")){
            throw IllegalArgumentException("Invalid name $dtoSchemaName for schema $allSchemas")
        }

        val allSchemasValue = allSchemas.substring(1 + dtoSchemaName.length + 2)

        val schemas = getMapStringFromSchemas(allSchemasValue)
        val dtoSchema = schemas[dtoSchemaName] ?: throw IllegalStateException("cannot find the schema with $dtoSchemaName from $allSchemas")

        if(dtoCache.containsKey(dtoSchema)){
            return dtoCache[dtoSchema]!!.copy()
        }

        val schema = """
            {
                "openapi": "3.0.0",
                "$OPENAPI_COMPONENT_NAME": {
                    "$OPENAPI_SCHEMA_NAME": $allSchemasValue
                }
            }          
        """.trimIndent()

        //FIXME here we are swallowing all error messages in schema
        //FIXME duplicated code
        val swagger = OpenAPIParser().readContents(schema,null,null)
        val currentSchema = SchemaOpenAPI(schema,swagger.openAPI, SchemaLocation.MEMORY)
        val schemaHolder = RestSchema(currentSchema)
        val converter = newConverter(schemaHolder, currentSchema, options)

        schemas.forEach { (t, u) ->
            val gene = converter.getGene(
                t,
                swagger.openAPI.components.schemas[t]!!,
                ArrayDeque(),
                referenceClassDef = t,
                messages = mutableListOf()
            )
            dtoCache[u] = gene
        }

        return dtoCache[dtoSchema]!!.copy()
    }

    /**
     * create gene based on dto schema
     *
     * @param name the name of gene
     * @param dtoSchema the schema of dto
     * @param referenceTypeName the name (eg, class name) of the reference type
     */
    fun createGeneForDTO(name: String,
                         dtoSchema: String,
                         referenceTypeName: String?,
                         options: Options
    ) : Gene{

        if(! dtoSchema.startsWith("\"$name\"")){
            throw IllegalArgumentException("Invalid name $name for schema $dtoSchema")
        }

        if(dtoCache.containsKey(dtoSchema)){
            return dtoCache[dtoSchema]!!.copy()
        }

        //Note to simplify code, we just create a whole OpenAPI schema
        val schema = """
            {
                "openapi": "3.0.0",
                "$OPENAPI_COMPONENT_NAME": {
                    "$OPENAPI_SCHEMA_NAME": {
                        $dtoSchema
                    }
                }
            }          
        """.trimIndent()

        //FIXME here we are swallowing all error messages in schema
        //FIXME duplicated code
        val swagger = OpenAPIParser().readContents(schema,null,null)
        val currentSchema = SchemaOpenAPI(schema,swagger.openAPI, SchemaLocation.MEMORY)
        val schemaHolder = RestSchema(currentSchema)
        val converter = newConverter(schemaHolder, currentSchema, options)
        val gene = converter.getGene(
            name,
            currentSchema.schemaParsed.components.schemas[name]!!,
            ArrayDeque(),
            referenceClassDef = referenceTypeName,
            messages = mutableListOf()
        )
        dtoCache[dtoSchema] = gene
        return gene.copy()
    }

    fun createGenesForDTOs(names: List<String>,
                           dtoSchemas: List<String>,
                           referenceTypeNames: List<String?>,
                           options: Options
    ) : List<Gene>{

        Lazy.assert { names.size == dtoSchemas.size }

        dtoSchemas.forEachIndexed { index, s ->
            if(! s.startsWith("\"${names[index]}\"")){
                throw IllegalArgumentException("Invalid name ${names[index]} for schema $s")
            }
        }


        val unidentified = dtoSchemas.mapIndexed { index, s -> index to s  }.filter{ !dtoCache.containsKey(it.second) }

        //Note to simplify code, we just create a whole OpenAPI schema
        val schema = """
            {
                "openapi": "3.0.0",
                "$OPENAPI_COMPONENT_NAME": {
                    "$OPENAPI_SCHEMA_NAME": {
                        ${unidentified.joinToString(",") { it.second }}
                    }
                }
            }          
        """.trimIndent()

        //FIXME here we are swallowing all error messages in schema
        //FIXME duplicated code
        val swagger = OpenAPIParser().readContents(schema,null,null)
        val currentSchema = SchemaOpenAPI(schema,swagger.openAPI, SchemaLocation.MEMORY)
        val schemaHolder = RestSchema(currentSchema)
        val converter = newConverter(schemaHolder, currentSchema, options)

        unidentified.forEach {s->
            val gene = converter.getGene(
                names[s.first],
                swagger.openAPI.components.schemas[names[s.first]]!!,
                ArrayDeque(),
                referenceClassDef = referenceTypeNames[s.first],
                messages = mutableListOf()
            )
            if (!dtoCache.containsKey(s.second))
                dtoCache[s.second] = gene
        }

        return dtoSchemas.map { dtoCache[it]!!.copy() }
    }


    private fun resolveResponse(
        schema: RestSchema,
        currentSchema: SchemaOpenAPI,
        responseOrRef: ApiResponse,
        messages: MutableList<String>
    ): ApiResponse? {

        val sref = responseOrRef.`$ref`
            ?: return responseOrRef

        return SchemaUtils.getReferenceResponse(schema,currentSchema,sref, messages)
    }

    private fun handleOperation(
        actionCluster: MutableMap<String, Action>,
        verb: HttpVerb,
        restPath: RestPath,
        operation: Operation,
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        converter: JsonSchemaToGeneConverter,
        options: Options,
        errorEndpoints: MutableList<String>,
        messages: MutableList<String>
    ) {

        try{
            val params = extractParams(verb, restPath, operation, schemaHolder,currentSchema, converter, options, messages)
            repairParams(params, restPath, messages)

            val produces = operation.responses?.values //different response objects based on HTTP code
                ?.asSequence()
                ?.mapNotNull { resolveResponse(schemaHolder,currentSchema, it,messages) }
                ?.filter { it.content != null && it.content.isNotEmpty() }
                //each response can have different media-types
                ?.flatMap { it.content.keys }
                ?.toSet() // remove duplicates
                ?.toList()
                ?: listOf()

            val actionId = "$verb$restPath"
            val links = operation.responses
                ?.filter { it.value.links != null && it.value.links.isNotEmpty() }
                ?.flatMap { res ->  res.value.links.map {
                    Triple(
                        res.key, // the status code, used as key to identify the response object
                        it.key,  // the name of the link
                        it.value) // the actual link definition
                }
                }
                ?.mapNotNull {
                    try {
                        val ref = it.third.`$ref`
                        val link = if (ref.isNullOrBlank()) {
                            it.third
                        } else {
                            SchemaUtils.getReferenceLink(schemaHolder,currentSchema, ref, messages)
                        }
                        if (link == null) {
                            null
                        } else {
                            RestLink(
                                statusCode = it.first.toInt(),
                                name = it.second,
                                operationId = link.operationId,
                                operationRef = link.operationRef,
                                parameterDefinitions = link.parameters ?: mapOf(),
                                requestBody = link.requestBody?.toString(),
                                server = link.server?.toString()
                            )
                        }
                    }
                    catch (e: Exception){
                        messages.add("Failed to handle link definition ${it.second}: ${e.message}")
                        null
                    }
                } ?: listOf()

            val action = RestCallAction(actionId, verb, restPath, params, produces = produces,
                operationId = operation.operationId, links = links
            )

            if (options.doParseDescription) {
                var info = operation.description
                if (!info.isNullOrBlank() && !info.endsWith(".")) info += "."
                if (!operation.summary.isNullOrBlank()) info = if (info == null) operation.summary else (info + " " + operation.summary)
                if (!info.isNullOrBlank() && !info.endsWith(".")) info += "."
                action.initTokens(info)
            }

            actionCluster[action.getName()] = action
        }catch (e: Exception){
            messages.add("Fail to handle endpoint $verb$restPath due to ${e.message}")
            errorEndpoints.add("$verb$restPath")
        }
    }


    private fun extractParams(
        verb: HttpVerb,
        restPath: RestPath,
        operation: Operation,
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        converter: JsonSchemaToGeneConverter,
        options: Options,
        messages: MutableList<String>
    ): MutableList<Param> {

        val params = mutableListOf<Param>()

        removeDuplicatedParams(schemaHolder,currentSchema,operation,messages)
            .forEach { p ->

                if(p.`$ref` != null){
                    val param = SchemaUtils.getReferenceParameter(schemaHolder,currentSchema, p.`$ref`, messages)
                    if(param == null){
                        messages.add("Failed to handle ${p.`$ref`} in $verb:$restPath")
                    } else {
                        handleParam(param, schemaHolder, currentSchema, converter, params, options, messages)
                    }
                } else {
                    handleParam(p, schemaHolder,currentSchema, converter, params, options, messages)
                }
            }

        handleBodyPayload(operation, verb, restPath, schemaHolder, currentSchema, converter, params, options, messages)

        return params
    }

    private fun exampleObjects(
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        example: Any?,
        examples: Map<String, Example>?,
        messages: MutableList<String>
    ) : List<Pair<Any,String?>>{

        /**
         * List of pairs value/name.
         * the name if optional, as only defined for "examples"
         */
        val data = mutableListOf<Pair<Any, String?>>()

        if(example != null){
            data.add(Pair(example,null))
        }
        if(!examples.isNullOrEmpty()){
            examples.entries.forEach {
                val exm = if(it.value.`$ref` != null){
                    SchemaUtils.getReferenceExample(schemaHolder, currentSchema, it.value.`$ref`, messages)
                } else {
                    it.value
                }
                if(exm != null) {
                    data.add(Pair(exm.value, it.key))
                }
            }
        }
        return data
    }

    private fun handleParam(p: Parameter,
                            schemaHolder: RestSchema,
                            currentSchema: SchemaOpenAPI,
                            converter: JsonSchemaToGeneConverter,
                            params: MutableList<Param>,
                            options: Options,
                            messages: MutableList<String>
    ) {
        val name = p.name ?: "undefined"
        val description = p.description

        if(p.schema == null){
            messages.add("No schema definition for parameter $name")
            return
        }

        val examples = if(options.probUseExamples > 0) {
            exampleObjects(schemaHolder, currentSchema, p.example, p.examples, messages)
        } else {
            listOf()
        }

        var gene = converter.getGene(
            name,
            p.schema,
            referenceClassDef = null,
            isInPath = p.`in` == "path",
            examples = examples,
            messages = messages
        )

        if (p.required != true && p.`in` != "path" && gene !is OptionalGene) {
            // As of V3, "path" parameters must be required
            gene = OptionalGene(name, gene)
        }

        // TODO: Adding description to the parameter occurs in multiple places. This can be refactored.
        when (p.`in`) {
            "query" -> params.add(QueryParam(name, gene, p.explode ?: true, p.style ?: Parameter.StyleEnum.FORM)
                .apply { this.description = description })
            /*
                a path is inside a Disruptive Gene, because there are cases in which we want to prevent
                mutation. Note that 1.0 means can always be mutated
             */
            "path" -> params.add(PathParam(name, CustomMutationRateGene(name, gene, 1.0))
                .apply { this.description = description }
            )
            "header" -> params.add(HeaderParam(name, gene).apply { this.description = description })
            "cookie" -> params // do nothing?
            //TODO "cookie" does it need any special treatment? as anyway handled in auth configs
            else -> throw IllegalStateException("Unrecognized: ${p.getIn()}")
        }
    }

    /**
     * Have seen some cases of (old?) Swagger wrongly marking path params as query params
     */
    private fun repairParams(params: MutableList<Param>, restPath: RestPath, messages: MutableList<String>) {

        restPath.getVariableNames().forEach { n ->

            val p = params.find { p -> p is PathParam && p.name == n }
            if (p == null) {
                messages.add("No path parameter for variable '$n' in $restPath")

                //this could happen if bug in Swagger
                var fixed = false
                for (i in 0 until params.size) {
                    if (params[i] is QueryParam && params[i].name == n) {
                        params[i] = PathParam(params[i].name, CustomMutationRateGene(params[i].gene.name, params[i].gene, 1.0))
                        fixed = true
                        break
                    }
                }

                if (!fixed) {
                    //just create a string
                    val k = PathParam(n, CustomMutationRateGene(n, StringGene(n), 1.0))
                    params.add(k)
                }
            }
        }
    }

    private fun handleBodyPayload(
        operation: Operation,
        verb: HttpVerb,
        restPath: RestPath,
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        converter: JsonSchemaToGeneConverter,
        params: MutableList<Param>,
        options: Options,
        messages: MutableList<String>
    ) {

        // Return early if requestBody is missing
        val body = operation.requestBody ?: return

        if (operation.requestBody == null) {
            return
        }

        if (!listOf(HttpVerb.POST, HttpVerb.PATCH, HttpVerb.PUT).contains(verb)) {
            messages.add("Issue in $restPath: in OpenAPI, body payloads are not allowed for $verb," +
                    " although they are technically valid for HTTP (RFC 9110).")
            //https://swagger.io/docs/specification/describing-request-body/
            //https://www.rfc-editor.org/rfc/rfc9110.html#section-9.3.1-6
        }

        // Handle dereferencing if requestBody is referenced
        val resolvedBody = if (body.`$ref` != null) {
            SchemaUtils.getReferenceRequestBody(schemaHolder,currentSchema, body.`$ref`, messages) ?: return
        } else {
            body
        }

        val description = operation.description ?: null

        val bodies = resolvedBody.content?.filter {
            /*
                If it is a reference, then it must be present.
                Had issue with SpringFox in Proxyprint generating wrong schemas
                when WebRequest and Principal are used
             */
            if (it.value.schema == null) {
                false
            } else {
                val reference = it.value.schema.`$ref`
                reference.isNullOrBlank()
                        || SchemaUtils.getReferenceSchema(schemaHolder,currentSchema, reference,messages) != null
            }
        } ?: emptyMap()

        if (bodies.isEmpty()) {
            messages.add("No valid body-payload for $verb:$restPath")
            /*
                This will/should be handled by Testability Transformations at runtime.
                So we just default to a string map
             */
            return
        }


        /*
            FIXME as of V3, different types might have different body definitions.
            This should refactored to enable possibility of different BodyParams
        */
        val obj: MediaType = bodies.values.first()
        val examples = if(options.probUseExamples > 0) {
            exampleObjects(schemaHolder, currentSchema, obj.example, obj.examples, messages)
        } else {
            listOf()
        }

        // $ref schemas do not carry XML metadata; resolving the reference is required to obtain the correct XML element name from the target schema
        val deref = obj.schema.`$ref`?.let { ref -> SchemaUtils.getReferenceSchema(schemaHolder, currentSchema, ref, messages) } ?: obj.schema
        val name = deref?.xml?.name ?: deref?.`$ref`?.substringAfterLast("/") ?: "body"

        var gene = converter.getGene(name, obj.schema, referenceClassDef = null, messages = messages, examples = examples)

        if (resolvedBody.required != true && gene !is OptionalGene) {
            gene = OptionalGene(name, gene)
        }

        val contentTypeGene = EnumGene<String>("contentType", bodies.keys)
        val bodyParam = BodyParam(gene, contentTypeGene)
            .apply { this.description = description }

        val ns = bodyParam.notSupportedContentTypes
        if(ns.isNotEmpty()){
            messages.add("Not supported content types for body payload in $verb:$restPath : ${ns.joinToString()}")
        }

        params.add(bodyParam)
    }


    private fun removeDuplicatedParams(
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        operation: Operation,
        messages: MutableList<String>
    ): List<Parameter> {

        /*
            Duplicates are not allowed, based on combination of "name" and "location".
            https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#operationObject
         */

        if (operation.parameters == null) {
            return listOf()
        }

        val selection = mutableListOf<Parameter>()
        val seen = mutableSetOf<String>()
        val duplicates = mutableSetOf<String>()

        operation.parameters.forEach {

            val p = if(it.`$ref` != null)
                SchemaUtils.getReferenceParameter(schemaHolder,currentSchema, it.`$ref`, messages = messages)
            else
                it
            if(p != null) {
                val key = p.`in` + "_" + p.name
                if (!seen.contains(key)) {
                    seen.add(key)
                    selection.add(p)
                } else {
                    duplicates.add(key)
                }
            }
        }

        if (duplicates.isNotEmpty()) {
            messages.add("Operation ${operation.operationId} has ${duplicates.size} repeated parameters: ${duplicates.joinToString()}")
        }

        return selection
    }


    @Deprecated("should be removed, no longer used")
    fun getModelsFromSwagger(swagger: OpenAPI,
                             modelCluster: MutableMap<String, ObjectGene>,
                             options: Options
    ) {
//        modelCluster.clear()
//
//        /*
//            needs to check whether there exist some side-effects
//            if do not clean those, some testDeterminism might fail due to inconsistent warning log.
//         */
//        refCache.clear()
//        dtoCache.clear()
//
//        if (swagger.components?.schemas != null) {
//            swagger.components.schemas
//                    .forEach {
//                        val model = createObjectFromReference(it.key,
//                                it.component1(),
//                                schemaHolder,
//                                currentSchema,
//                                options = options,
//                                examples = listOf(),
//                                messages = mutableListOf()
//                        )
//                        when (model) {
//                            //BMR: the modelCluster expects an ObjectGene. If the result is not that, it is wrapped in one.
//                            is ObjectGene -> modelCluster[it.component1()] = model
//                            //is MapGene<*, *> -> modelCluster.put(it.component1(), ObjectGene(it.component1(), listOf(model)))
//                            //Andrea: this was wrong, as generating invalid genes where writing expectations.
//                            // this is a tmp fix
//                            is FixedMapGene<*, *> -> modelCluster[it.component1()] = ObjectGene(it.component1(), listOf())
//                        }
//
//                    }
//        }
    }

    fun getBasePathFromURL(swagger: OpenAPI): String {
        /*
            TODO would need more general approach, as different HTTP servers could
            have different base paths
         */
        val serverUrl = swagger.servers[0].url
        val basePath: String = try {
            URI(serverUrl).path.trim()
        } catch (e: URISyntaxException) {
            LoggingUtil.uniqueWarn(log, "Invalid URI used in schema to define servers: $serverUrl")
            ""
        }
        return basePath
    }

    /**
     * build a rest action based on the given [url]
     */
    fun buildActionBasedOnUrl(baseUrl: String, id : String, verb: HttpVerb, url: String, skipOracleChecks : Boolean) : RestCallAction?{

        // if the url does not start with baseUrl (i.e., not from SUT), then there might be no point to execute this rest action
        if (!url.startsWith(baseUrl)) return null

        // fragments # are ignored when making HTTP calls
        val uri = URI(url.replaceAfter("#","").removeSuffix("#"))
//        Lazy.assert { "${uri.scheme}://${uri.host}:${uri.port}" == baseUrl }

        val path = RestPath("${uri.scheme}://${uri.host}:${uri.port}${uri.path}".removePrefix(baseUrl.removeSuffix("/")))
        val query : MutableList<Param> = uri.query?.split("&")?.map { q->
            val keyValue = q.split("=")
            if (keyValue.size == 2 && keyValue[0].isNotBlank())
                QueryParam(keyValue[0], StringGene(keyValue[0], keyValue[1]))
            else {
                /*
                    key-value pair is not restricted for query
                    eg, /v2/api-docs?foo is considered as valid
                    see https://datatracker.ietf.org/doc/html/rfc3986#section-3.4
                 */
                log.warn("Currently not supporting a GET RestAction with the url '$url' ," +
                        " as all query parameters should be in the form key=value")
                return null
            }
        }?.toMutableList()?: mutableListOf()
        return RestCallAction(id, verb, path, query, skipOracleChecks= skipOracleChecks)
    }

    private fun getMapStringFromSchemas(schemas: String) : Map<String, String>{
        val objs = mapper.readTree(schemas)
        val maps = mutableMapOf<String, String>()
        objs.fields().forEach { f->
            maps[f.key] = f.value.toString()
        }

        return maps
    }

}
