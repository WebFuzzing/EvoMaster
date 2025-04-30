package org.evomaster.core.problem.rest.builder

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.JsonSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import org.evomaster.client.java.instrumentation.shared.ClassToSchemaUtils.OPENAPI_COMPONENT_NAME
import org.evomaster.client.java.instrumentation.shared.ClassToSchemaUtils.OPENAPI_SCHEMA_NAME
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.StaticCounter
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.parser.RegexHandler
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
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.*
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.FormatForDatesAndTimes
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.optional.ChoiceGene
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.placeholder.LimitObjectGene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.string.Base64StringGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import kotlin.math.max

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

    private val refCache = mutableMapOf<String, Gene>()

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

        val probUseExamples: Double = 0.0
    ){
        constructor(config: EMConfig): this(
            enableConstraintHandling = config.enableSchemaConstraintHandling,
            invalidData = config.allowInvalidData,
            probUseDefault = config.probRestDefault,
            probUseExamples = config.probRestExamples
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
        refCache.clear()
        dtoCache.clear()
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
            handlePathItem(pathKey, pi, messages,endpointsToSkip,skipped,actionCluster,schemaHolder,options,errorEndpoints)
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

        schemas.forEach { (t, u) ->
            val gene = getGene(t,
                swagger.openAPI.components.schemas[t]!!,
                schemaHolder,
                currentSchema,
                ArrayDeque(),
                t,
                options,
                messages = mutableListOf())
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
        val gene = createObjectGene(
            name,
            currentSchema.schemaParsed.components.schemas[name]!!,
            schemaHolder,
            currentSchema,
            ArrayDeque(),
            referenceTypeName,
            options,
            listOf(),
            mutableListOf())
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

        unidentified.forEach {s->
            val gene = getGene(names[s.first],
                swagger.openAPI.components.schemas[names[s.first]]!!,schemaHolder, currentSchema,
                ArrayDeque(), referenceTypeNames[s.first], options, messages = mutableListOf())
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
        options: Options,
        errorEndpoints: MutableList<String>,
        messages: MutableList<String>
    ) {

        try{
            val params = extractParams(verb, restPath, operation, schemaHolder,currentSchema, options, messages)
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
                operationId = operation.operationId, links = links)

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
                            handleParam(param, schemaHolder, currentSchema, params, options, messages)
                        }
                    } else {
                        handleParam(p, schemaHolder,currentSchema, params, options, messages)
                    }
                }

        handleBodyPayload(operation, verb, restPath, schemaHolder, currentSchema, params, options, messages)

        return params
    }

    private fun exampleObjects(
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        example: Any?,
        examples: Map<String, Example>?,
        messages: MutableList<String>
    ) : List<Any>{

        val data = mutableListOf<Any>()
        if(example != null){
            data.add(example)
        }
        if(!examples.isNullOrEmpty()){
            examples.values.forEach {
                val exm = if(it.`$ref` != null){
                    SchemaUtils.getReferenceExample(schemaHolder, currentSchema, it.`$ref`, messages)
                } else {
                    it
                }
                if(exm != null) {
                    data.add(exm.value)
                }
            }
        }
        return data
    }

    private fun handleParam(p: Parameter,
                            schemaHolder: RestSchema,
                            currentSchema: SchemaOpenAPI,
                            params: MutableList<Param>,
                            options: Options,
                            messages: MutableList<String>
    ) {
        val name = p.name ?: "undefined"

        if(p.schema == null){
            messages.add("No schema definition for parameter $name")
            return
        }

        val examples = if(options.probUseExamples > 0) {
            exampleObjects(schemaHolder, currentSchema, p.example, p.examples, messages)
        } else {
            listOf()
        }

        var gene = getGene(
            name,
            p.schema,
            schemaHolder,
            currentSchema,
            referenceClassDef = null,
            options = options,
            isInPath = p.`in` == "path",
            examples = examples,
            messages = messages
        )

        if (p.required != true && p.`in` != "path" && gene !is OptionalGene) {
            // As of V3, "path" parameters must be required
            gene = OptionalGene(name, gene)
        }

        when (p.`in`) {

            "query" -> {
                params.add(QueryParam(name, gene, p.explode ?: true, p.style ?: Parameter.StyleEnum.FORM))
            }
            /*
                a path is inside a Disruptive Gene, because there are cases in which we want to prevent
                mutation. Note that 1.0 means can always be mutated
             */
            "path" -> params.add(PathParam(name, CustomMutationRateGene("d_", gene, 1.0)))
            "header" -> params.add(HeaderParam(name, gene))
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
                        params[i] = PathParam(params[i].name, CustomMutationRateGene("d_", params[i].gene, 1.0))
                        fixed = true
                        break
                    }
                }

                if (!fixed) {
                    //just create a string
                    val k = PathParam(n, CustomMutationRateGene("d_", StringGene(n), 1.0))
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
            if(verb == HttpVerb.GET){
                //currently cannot handle it due to bud in Jersey :(
                //TODO check once upgrading Jersey
                //TODO update AbstractRestFitness accordingly
                return
            }
        }

        // Handle dereferencing if requestBody is referenced
        val resolvedBody = if (body.`$ref` != null) {
            SchemaUtils.getReferenceRequestBody(schemaHolder,currentSchema, body.`$ref`, messages) ?: return
        } else {
            body
        }

        val name = "body"

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

        var gene = getGene("body", obj.schema, schemaHolder,currentSchema, referenceClassDef = null, options = options, messages = messages, examples = examples)


        if (resolvedBody.required != true && gene !is OptionalGene) {
            gene = OptionalGene(name, gene)
        }

        val contentTypeGene = EnumGene<String>("contentType", bodies.keys)
        val bodyParam = BodyParam(gene, contentTypeGene)
        val ns = bodyParam.notSupportedContentTypes
        if(ns.isNotEmpty()){
            messages.add("Not supported content types for body payload in $verb:$restPath : ${ns.joinToString()}")
        }

        params.add(bodyParam)
    }

    private fun possiblyOptional(gene: Gene, required: Boolean?): Gene {

        if (required != true) {
            return OptionalGene(gene.name, gene).also { GeneUtils.preventCycles(it) }
        }

        return gene
    }

    private fun getGene(
        name: String,
        schema: Schema<*>,
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        history: Deque<String> = ArrayDeque(),
        referenceClassDef: String?,
        options: Options,
        isInPath: Boolean = false,
        examples: List<Any> = listOf(),
        messages: MutableList<String>
    ): Gene {

        if (!schema.`$ref`.isNullOrBlank()) {
            return createObjectFromReference(name, schema.`$ref`, schemaHolder,currentSchema, history, options, examples, messages)
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

        val type = schema.type?:schema.types?.firstOrNull()
        val format = schema.format

        if (schema.enum?.isNotEmpty() == true) {

            when (type) {
                "string" ->
                    return EnumGene(name, (schema.enum.map {
                        if (it !is String)
                            LoggingUtil.uniqueWarn(log, "an item of enum is not string (ie, ${it::class.java.simpleName}) for a property whose `type` is string and `name` is $name")
                        it.toString()
                    } as MutableList<String>).apply {
                        if(options.invalidData) {
                            //Besides the defined values, add one to test robustness
                            add("EVOMASTER")
                        }
                    })
                /*
                    Looks like a possible bug in the parser, where numeric enums can be read as strings... got this
                    issue in GitLab schemas, eg for visibility_level
                 */
                "integer" -> {
                    if (format == "int64") {
                        val data : MutableList<Long> = schema.enum
                                .map{ if(it is String) it.toLong() else it as Long}
                                .toMutableList()

                        return EnumGene(name, (data).apply { add(42L) })
                    }

                    val data : MutableList<Int> = schema.enum
                            .map{ if(it is String) it.toInt() else it as Int}
                            .toMutableList()
                    return EnumGene(name, data.apply { add(42) })
                }
                "number" -> {
                    //if (format == "double" || format == "float") {
                    //TODO: Is it always casted as Double even for Float??? Need test
                    val data : MutableList<Double> = schema.enum
                            .map{ if(it is String) it.toDouble() else it as Double}
                            .toMutableList()
                    return EnumGene(name, data.apply { add(42.0) })
                }
                else -> messages.add("Cannot handle enum of type: $type")
            }
        }


        //first check for "optional" format
        when (format?.lowercase()) {
            "char" -> return buildStringGeneForChar(name, isInPath)
            "int8","int16","int32" -> return createNonObjectGeneWithSchemaConstraints(schema, name, IntegerGene::class.java, options, null, isInPath, examples,format, messages)//IntegerGene(name)
            "int64" -> return createNonObjectGeneWithSchemaConstraints(schema, name, LongGene::class.java, options, null, isInPath, examples, messages = messages) //LongGene(name)
            "double" -> return createNonObjectGeneWithSchemaConstraints(schema, name, DoubleGene::class.java, options, null, isInPath, examples, messages = messages)//DoubleGene(name)
            "float" -> return createNonObjectGeneWithSchemaConstraints(schema, name, FloatGene::class.java, options, null, isInPath, examples, messages = messages)//FloatGene(name)
            "password" -> return createNonObjectGeneWithSchemaConstraints(schema, name, StringGene::class.java, options, null, isInPath, examples, messages = messages)//StringGene(name) //nothing special to do, it is just a hint
            "binary" -> return createNonObjectGeneWithSchemaConstraints(schema, name, StringGene::class.java, options, null, isInPath, examples, messages = messages)//StringGene(name) //does it need to be treated specially?
            "byte" -> return createNonObjectGeneWithSchemaConstraints(schema, name, Base64StringGene::class.java, options, null, isInPath, examples, messages = messages)//Base64StringGene(name)
            "date", "local-date" -> return DateGene(name, onlyValidDates = !options.invalidData)
            "date-time", "local-date-time" -> {
                val f = if(format?.lowercase() == "date-time"){
                    FormatForDatesAndTimes.RFC3339
                } else {
                    FormatForDatesAndTimes.ISO_LOCAL
                }
                return DateTimeGene(
                    name,
                    format = f,
                    date = DateGene("date", onlyValidDates = !options.invalidData, format = f),
                    time = TimeGene("time", onlyValidTimes = !options.invalidData, format = f)
                )
            }
            else -> if (format != null) {
                messages.add("Unhandled format '$format' for '$name'")
            }
        }

        /*
                If a format is not defined, the type should default to
                the JSON Schema definition
         */
        when (type?.lowercase()) {
            "integer" -> return createNonObjectGeneWithSchemaConstraints(schema, name, IntegerGene::class.java, options, null, isInPath, examples, messages = messages)//IntegerGene(name)
            "number" -> return createNonObjectGeneWithSchemaConstraints(schema, name, DoubleGene::class.java, options, null, isInPath, examples, messages = messages)//DoubleGene(name)
            "boolean" -> return BooleanGene(name)
            "string" -> {
                return if (schema.pattern == null) {
                    createNonObjectGeneWithSchemaConstraints(schema, name, StringGene::class.java, options, null, isInPath, examples, messages = messages) //StringGene(name)
                } else {
                    try {
                        createNonObjectGeneWithSchemaConstraints(schema, name, RegexGene::class.java, options, null, isInPath, examples, messages = messages)
                    } catch (e: Exception) {
                        /*
                            TODO: if the Regex is syntactically invalid, we should warn
                            the user. But, as we do not support 100% regex, might be an issue
                            with EvoMaster. Anyway, in such cases, instead of crashing EM, let's just
                            take it as a String.
                            When 100% support, then tell user that it is his/her fault
                         */
                        LoggingUtil.uniqueWarn(log, "Cannot handle regex: ${schema.pattern}")
                        createNonObjectGeneWithSchemaConstraints(schema, name, StringGene::class.java, options, null, isInPath, examples, messages = messages)//StringGene(name)
                    }
                }
            }
            "array" -> {
                if (schema is ArraySchema || schema is JsonSchema) {

                    val arrayType: Schema<*> = if (schema.items == null) {
                        LoggingUtil.uniqueWarn(
                            log, "Array type '$name' is missing mandatory field 'items' to define its type." +
                                " Defaulting to 'string'")
                        Schema<Any>().also { it.type = "string" }
                    } else {
                        schema.items
                    }
                    val template = getGene(name + "_item", arrayType, schemaHolder,currentSchema, history, referenceClassDef = null, options = options, messages = messages)

                    //Could still have an empty []
//                    if (template is CycleObjectGene) {
//                        return CycleObjectGene("<array> ${template.name}")
//                    }
                    return createNonObjectGeneWithSchemaConstraints(schema, name, ArrayGene::class.java, options, template, isInPath, examples, messages = messages)//ArrayGene(name, template)
                } else {
                    LoggingUtil.uniqueWarn(log, "Invalid 'array' definition for '$name'")
                }
            }

            "object" -> {
                return createObjectGene(name, schema, schemaHolder,currentSchema, history, referenceClassDef, options, examples, messages)
            }
            //TODO file is a hack. I want to find a more elegant way of dealing with it (BMR)
            //FIXME is this even a standard type???
            "file" -> return createNonObjectGeneWithSchemaConstraints(schema, name, StringGene::class.java, options, null, isInPath, examples, messages = messages) //StringGene(name)
        }

        if ((name == "body" || referenceClassDef != null) && schema.properties?.isNotEmpty() == true) {
            /*
                name == "body": This could happen when parsing a body-payload as formData
                referenceClassDef != null : this could happen when parsing a reference of a constraint (eg, anyOf) of the additionalProperties
            */
            return createObjectGene(name, schema, schemaHolder,currentSchema, history, referenceClassDef, options, examples, messages)
        }

        if (type == null && format == null) {
            return createGeneWithUnderSpecificTypeAndSchemaConstraints(
                schema, name, schemaHolder,currentSchema, history, referenceClassDef,
                options, null, isInPath, examples, messages)
        //createNonObjectGeneWithSchemaConstraints(schema, name, StringGene::class.java, enableConstraintHandling) //StringGene(name)
        }

        throw IllegalArgumentException("Cannot handle combination $type/$format")
    }

    /**
     * @param referenceTypeName is the name of object type
     */
    private fun createObjectGene(name: String,
                                 schema: Schema<*>,
                                 schemaHolder: RestSchema,
                                 currentSchema: SchemaOpenAPI,
                                 history: Deque<String>,
                                 referenceTypeName: String?,
                                 options: Options,
                                 examples: List<Any>,
                                 messages: MutableList<String>
    ): Gene {

        val fields = schema.properties?.entries?.map {
            possiblyOptional(
                    getGene(it.key, it.value, schemaHolder,currentSchema, history, referenceClassDef = null, options = options, messages = messages),
                    schema.required?.contains(it.key)
            )
        } ?: listOf()


        /*
            additional properties could be

            from OpenAPI Specification v3.1.0 https://spec.openapis.org/oas/latest.html
            1 - A free-form query parameter, allowing undefined parameters of a specific type:
                eg, {
                      "in": "query",
                      "name": "freeForm",
                      "schema": {
                        "type": "object",
                        "additionalProperties": {
                          "type": "integer"
                        },
                      },
                      "style": "form"
                    }

            2 - Model with Map/Dictionary Properties
                eg, {
                      "type": "object",
                      "additionalProperties": {
                        "type": "string"
                      }
                    }

                    {
                      "type": "object",
                      "additionalProperties": {
                        "$ref": "#/components/schemas/ComplexModel"
                      }
                    }

             from https://json-schema.org/understanding-json-schema/reference/object.html
             3 - The additionalProperties keyword is used to control the handling of extra stuff,
             that is, properties whose names are not listed in the properties keyword
             or match any of the regular expressions in the patternProperties keyword.
             By default any additional properties are allowed.

             The value of the additionalProperties keyword is a schema that will be used to validate
             any properties in the instance that are not matched by properties or patternProperties.
             Setting the additionalProperties schema to false means no additional properties will be allowed.

             note that the latest version (ie, OpenAPI 3.1.0) does not support patternProperties yet
             see more with https://docs.readme.com/docs/openapi-compatibility-chart
         */

        var additionalFieldTemplate : PairGene<StringGene, Gene>? = null
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
               support additionalProperties with schema
            */
            if (!additional.`$ref`.isNullOrBlank()) {
                val valueTemplate = createObjectFromReference(
                    "valueTemplate",
                    additional.`$ref`,
                    schemaHolder,
                    currentSchema,
                    history,
                    options = options,
                    examples = examples,
                    messages = messages)
                additionalFieldTemplate= PairGene(
                    "template",
                    StringGene("keyTemplate"),
                    valueTemplate.copy())
            }else if(!additional.type.isNullOrBlank() || additional.types?.isNotEmpty() == true){
                val valueTemplate = getGene(
                    "valueTemplate",
                    additional,
                    schemaHolder,
                    currentSchema,
                    history,
                    null,
                    options = options,
                    messages = messages)
                additionalFieldTemplate = PairGene("template", StringGene("keyTemplate"), valueTemplate.copy())
            }

            /*
               TODO could add extra fields for robustness testing,
               with and without following the given schema for their type
             */

            /*
                TODO proper handling.
                Using a map is just a temp solution
             */

//            if (fields.isEmpty()) {
//                if (additionalFieldTemplate != null)
//                    return FixedMapGene(name, additionalFieldTemplate)
//
//                // here, the first of pairgene should not be mutable
//                return FixedMapGene(name, PairGene.createStringPairGene(getGene(name + "_field", additional, swagger, history, null, enableConstraintHandling), isFixedFirst = true))
//            }
        }

        return assembleObjectGeneWithConstraints(
            name,
            schema,
            fields,
            additionalFieldTemplate,
            schemaHolder,
            currentSchema,
            history,
            referenceTypeName,
            options,
            examples,
            messages
        )

    }

    /**
     *
     * handled constraints include
     *      - allOf
     *      - anyOf
     *      - oneOf
     *      - not (OpenAPI not support this yet)
     */
    private fun assembleObjectGeneWithConstraints(
        name: String,
        schema: Schema<*>,
        fields: List<Gene>,
        additionalFieldTemplate: PairGene<StringGene, Gene>?,
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        history: Deque<String>,
        referenceTypeName: String?,
        options: Options,
        examples: List<Any>,
        messages: MutableList<String>
    ) : Gene{
        /*
            TODO discriminator
            https://spec.openapis.org/oas/latest.html#discriminator-object
         */

        if (!options.enableConstraintHandling)
            return assembleObjectGene(name, options, schema, fields, additionalFieldTemplate, referenceTypeName, examples, messages)

        val allOf = schema.allOf?.map { s->
            //createObjectGene(name, s, swagger, history, null, enableConstraintHandling)
            getGene(name, s, schemaHolder,currentSchema, history, null, options, messages = messages, examples = examples)
        }

        val anyOf = schema.anyOf?.map { s->
            //createObjectGene(name, s, swagger, history, null, enableConstraintHandling)
            getGene(name, s, schemaHolder,currentSchema, history, null, options, messages = messages, examples = examples)
        }

        if (!allOf.isNullOrEmpty() && !anyOf.isNullOrEmpty()){
            messages.add("Cannot handle allOf and oneOf at same time for a schema with name $name")
            return assembleObjectGene(name, options, schema, fields, additionalFieldTemplate, referenceTypeName, examples, messages)
        }

        val oneOf = schema.oneOf?.map { s->
            //createObjectGene(name, s, swagger, history, null, enableConstraintHandling)
            getGene(name, s, schemaHolder,currentSchema, history, null, options = options, messages = messages)
        }

        if (!oneOf.isNullOrEmpty() && (!allOf.isNullOrEmpty() || !anyOf.isNullOrEmpty())){
            messages.add("cannot handle oneOf and allOf/oneOf at same time for a schema with name $name")
            return assembleObjectGene(name, options, schema, fields, additionalFieldTemplate, referenceTypeName, examples, messages)
        }

        if (!allOf.isNullOrEmpty()){
            val allFields = allOf.mapNotNull {
                when (it) {
                    is ObjectGene -> it.fields
                    else -> null
                }
            }.flatten()
            return assembleObjectGene(name, options, schema, allFields.plus(fields), additionalFieldTemplate, referenceTypeName, examples, messages)
        }

        if (!oneOf.isNullOrEmpty()){
            val choices = if (fields.isEmpty())
                oneOf
            else
                listOf(assembleObjectGene(name, options, schema, fields, additionalFieldTemplate, referenceTypeName, examples, messages)).plus(oneOf)

            return ChoiceGene(name, choices)
        }

        if (!anyOf.isNullOrEmpty()){
            val allFields = anyOf.mapNotNull {
                when (it) {
                    is ObjectGene -> it.fields
                    else -> null
                }
            }.flatten()
            /*
                currently, we handle anyOf as oneOf plus all combined one
             */
            return ChoiceGene(name, if (anyOf.size > 1) anyOf.plus(assembleObjectGene(name, options, schema, allFields.plus(fields), additionalFieldTemplate, referenceTypeName, examples, messages)) else anyOf)
//            /*
//                handle all combinations of anyOf
//                comment it out for the moment
//             */
//            return ChoiceGene(name, (1 until 2.0.pow(one.size * 1.0).toInt()).map { i->
//                assembleObjectGene(
//                        name,
//                        schema,
//                        Integer.toBinaryString(i).toCharArray().mapIndexed { index, c ->
//                            if (c == '1'){
//                                one[index].run {
//                                    when (this) {
//                                        is ObjectGene -> this.fields
//                                        is FlexibleObjectGene<*> -> this.fields
//                                        else -> null
//                                    }
//                                }
//                            }else null
//                        }.filterNotNull().flatten(),
//                        additionalFieldTemplate,
//                        referenceTypeName
//                )
//            })
        }

        return assembleObjectGene(name, options, schema, fields, additionalFieldTemplate, referenceTypeName, examples, messages)

        //TODO not
    }

    /**
     * assemble ObjectGene based on [fields] and [additionalFieldTemplate]
     */
    private fun assembleObjectGene(
        name: String,
        options: Options,
        schema: Schema<*>,
        fields: List<Gene>,
        additionalFieldTemplate: PairGene<StringGene, Gene>?,
        referenceTypeName: String?,
        otherExampleValues: List<Any>,
        messages: MutableList<String>
    ) : Gene{
        if (fields.isEmpty()) {
            if (additionalFieldTemplate != null)
                return FixedMapGene(name, additionalFieldTemplate)

            messages.add("No fields for object definition: $name")

            return if(schema.additionalProperties == null || (schema.additionalProperties is Boolean && schema.additionalProperties == true)) {
                //default is true
                TaintedMapGene(name, TaintInputName.getTaintName(StaticCounter.getAndIncrease()))
            } else {
                /*
                            If we get here, it is really something wrong with the schema...
                         */
                FixedMapGene(name, PairGene.createStringPairGene(StringGene(name + "_field"), isFixedFirst = true))
            }
        }

        val mainGene = if (additionalFieldTemplate!=null){
            ObjectGene(name, fields, if(schema is ObjectSchema) referenceTypeName?:schema.title else null, false, additionalFieldTemplate, mutableListOf())
        } else {
            ObjectGene(name, fields, if(schema is ObjectSchema) referenceTypeName?:schema.title else null)
        }

        val defaultValue = if(options.probUseDefault > 0) schema.default else null
        val exampleValue = if(options.probUseExamples > 0) schema.example else null
        val multiExampleValues = if(options.probUseExamples > 0) schema.examples else null

        val examples = mutableListOf<ObjectGene>()
        if(exampleValue != null){
            duplicateObjectWithExampleFields(name,mainGene, exampleValue)?.let {
                examples.add(it)
            }
        }
        if(multiExampleValues != null ){
            examples.addAll(multiExampleValues.mapNotNull { duplicateObjectWithExampleFields(name,mainGene, it) })
        }
        examples.addAll(otherExampleValues.mapNotNull { duplicateObjectWithExampleFields(name,mainGene, it) })

        val exampleGene = if(examples.isNotEmpty()){
            ChoiceGene(EXAMPLES_NAME, examples)
        } else null
        val defaultGene = if(defaultValue != null){
            duplicateObjectWithExampleFields("default", mainGene, defaultValue)
        } else null

        /*
            add refClass with title of SchemaObject
            Man: shall we pop history here?
         */
       return createGeneWithExampleAndDefault(exampleGene,defaultGene,mainGene,options,name)
    }

    private fun duplicateObjectWithExampleFields(name: String, mainGene: ObjectGene, exampleValue: Any): ObjectGene? {

        if(exampleValue !is ObjectNode){
            LoggingUtil.uniqueWarn(log, "When building object example, required an ObjectNode, but found a ${exampleValue.javaClass}")
            return null
        }

        val modified = mainGene.fields.map { f ->
            if(exampleValue.has(f.name)){
                val e = exampleValue.get(f.name)
                if(e.isTextual){
                    EnumGene<String>(f.name, listOf(asRawString(e.textValue())), 0, false)
                } else if(e.isObject) {
                    val nested = f.getWrappedGene(ObjectGene::class.java)
                    if(nested == null){
                        LoggingUtil.uniqueWarn(log, "When building object example, cannot handle nested object due to gene type: ${f.javaClass}")
                        f.copy()
                    } else {
                        duplicateObjectWithExampleFields(f.name, nested, e)
                            ?: f.copy()
                    }
                } else {
                    EnumGene<String>(f.name, listOf(""+e.toString()), 0, true)
                }
            } else {
                /*
                    TODO: if a parameter is optional, and was not specified in the example,
                    should it be rather skipped? maybe, maybe not... unsure
                 */
                f.copy()
            }
        }
        return ObjectGene(
            name,
            modified,
            mainGene.refType,
            mainGene.isFixed,
            mainGene.template?.copy() as PairGene<StringGene,Gene>?,
            mainGene.additionalFields?.map { it.copy() as PairGene<StringGene,Gene>}?.toMutableList()
            )
    }

    /**
     * handle constraints of schema object
     *
     * see more
     * https://spec.openapis.org/oas/v3.0.3#properties
     * https://spec.openapis.org/oas/latest.html#properties
     *
     * handled constraints include
     *      - minimum (handled by createNonObjectGeneWithSchemaConstraints)
     *      - maximum (handled by createNonObjectGeneWithSchemaConstraints)
     *      - exclusiveMaximum (handled by createNonObjectGeneWithSchemaConstraints)
     *      - exclusiveMinimum (handled by createNonObjectGeneWithSchemaConstraints)
     *      - maxLength (handled by createNonObjectGeneWithSchemaConstraints)
     *      - minLength (handled by createNonObjectGeneWithSchemaConstraints)
     *      - minItems (handled by createNonObjectGeneWithSchemaConstraints)
     *      - maxItems (handled by createNonObjectGeneWithSchemaConstraints)
     *      - uniqueItems (handled by createNonObjectGeneWithSchemaConstraints)
     *      - multipleOf (TODO)
     *      - allOf (handled by [assembleObjectGeneWithConstraints])
     *      - anyOf (handled by [assembleObjectGeneWithConstraints])
     *      - oneOf (handled by [assembleObjectGeneWithConstraints])
     *      - not (OpenAPI not support this yet)
     *
     *
     * TODO Man might handle example/default property as default later
     */
    private fun createGeneWithUnderSpecificTypeAndSchemaConstraints(
        schema: Schema<*>,
        name: String,
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        history: Deque<String>,
        referenceTypeName: String?,
        options: Options,
        collectionTemplate: Gene?,
        isInPath: Boolean,
        examples: List<Any>,
        messages: MutableList<String>
    ) : Gene{

        val mightObject = schema.properties?.isNotEmpty() == true || referenceTypeName != null || containsAllAnyOneOfConstraints(schema)
        if (mightObject){
            try {
                return createObjectGene(name, schema, schemaHolder,currentSchema, history, referenceTypeName, options, examples,  messages)
            }catch (e: Exception){
                LoggingUtil.uniqueWarn(log, "fail to create ObjectGene for a schema whose `type` and `format` are under specified with error msg: ${e.message?:"no msg"}")
            }
        }

        LoggingUtil.uniqueWarn(log, "No type/format information provided for '$name'. Defaulting to 'string'")
        return createNonObjectGeneWithSchemaConstraints(schema, name, StringGene::class.java, options = options, collectionTemplate,isInPath, listOf(), messages = messages)
    }

    private fun containsAllAnyOneOfConstraints(schema: Schema<*>) = schema.oneOf != null || schema.anyOf != null || schema.allOf != null

    /**
     * handled constraints include
     *      - minimum
     *      - maximum
     *      - exclusiveMaximum
     *      - exclusiveMinimum
     *      - maxLength
     *      - minLength
     *      - minItems
     *      - maxItems
     *      - uniqueItems
     *
     */
    private fun createNonObjectGeneWithSchemaConstraints(
        schema: Schema<*>,
        name: String,
        geneClass: Class<*>,
        options: Options,
        collectionTemplate: Gene? = null,
        //might need to add extra constraints if in path
        isInPath: Boolean,
        exampleObjects: List<Any>,
        format: String? = null,
        messages: MutableList<String>
    ) : Gene{


        val maxInclusive =  if (options.enableConstraintHandling) !(schema.exclusiveMaximum?:false) else true
        val minInclusive = if (options.enableConstraintHandling) !(schema.exclusiveMinimum?:false) else true

        val mainGene = when(geneClass){
            // number gene
            IntegerGene::class.java ->
            {
                val minRange: Int
                val maxRange: Int
                if (format == "int8") {
                    minRange = Byte.MIN_VALUE.toInt()
                    maxRange = Byte.MAX_VALUE.toInt()
                } else if (format == "int16") {
                    minRange = Short.MIN_VALUE.toInt()
                    maxRange = Short.MAX_VALUE.toInt()
                } else {
                    minRange = Integer.MIN_VALUE
                    maxRange = Integer.MAX_VALUE
                }

                val minConstraint: Int?
                val maxConstraint: Int?
                if (options.enableConstraintHandling) {
                    minConstraint = schema.minimum?.intValueExact()
                    maxConstraint = schema.maximum?.intValueExact()
                } else {
                    minConstraint = null
                    maxConstraint = null
                }

                val minValue = if (minConstraint != null) maxOf(minConstraint, minRange) else minRange
                val maxValue = if (maxConstraint != null) minOf(maxConstraint, maxRange) else maxRange

                IntegerGene(
                    name,
                    min = minValue,
                    max = maxValue,
                    maxInclusive = maxInclusive,
                    minInclusive = minInclusive
                )
            }
            LongGene::class.java -> LongGene(
                    name,
                    min = if (options.enableConstraintHandling) schema.minimum?.longValueExact() else null,
                    max = if (options.enableConstraintHandling) schema.maximum?.longValueExact() else null,
                    maxInclusive = maxInclusive,
                    minInclusive = minInclusive
            )
            FloatGene::class.java -> FloatGene(
                    name,
                    min = if (options.enableConstraintHandling) schema.minimum?.toFloat() else null,
                    max = if (options.enableConstraintHandling) schema.maximum?.toFloat() else null,
                    maxInclusive = maxInclusive,
                    minInclusive = minInclusive
            )
            DoubleGene::class.java -> DoubleGene(
                    name,
                    min = if (options.enableConstraintHandling) schema.minimum?.toDouble() else null,
                    max = if (options.enableConstraintHandling) schema.maximum?.toDouble() else null,
                    maxInclusive = maxInclusive,
                    minInclusive = minInclusive
            )
            BigDecimalGene::class.java ->  BigDecimalGene(
                    name,
                    min = if (options.enableConstraintHandling) schema.minimum else null,
                    max = if (options.enableConstraintHandling) schema.maximum else null,
                    maxInclusive = maxInclusive,
                    minInclusive = minInclusive
            )
            BigIntegerGene::class.java -> BigIntegerGene(
                    name,
                    min = if (options.enableConstraintHandling) schema.minimum?.toBigIntegerExact() else null,
                    max = if (options.enableConstraintHandling) schema.maximum?.toBigIntegerExact() else null,
                    maxInclusive = maxInclusive,
                    minInclusive = minInclusive
            )
            // string, Base64StringGene and regex gene
            StringGene::class.java -> buildStringGene(name, options, schema, isInPath)
            Base64StringGene::class.java ->  Base64StringGene(name, buildStringGene(name, options, schema, isInPath))
            RegexGene::class.java -> {
                /*
                    TODO handle constraints for regex gene
                    eg,  min and max
                    also, isInPath
                 */
                 RegexHandler.createGeneForEcma262(schema.pattern).apply { this.name = name }
            }
            ArrayGene::class.java -> {
                if (collectionTemplate == null)
                    throw IllegalArgumentException("cannot create ArrayGene when collectionTemplate is null")
                ArrayGene(
                        name,
                        template = collectionTemplate,
                        uniqueElements = if (options.enableConstraintHandling) schema.uniqueItems?:false else false,
                        minSize = if (options.enableConstraintHandling) schema.minItems else null,
                        maxSize = if (options.enableConstraintHandling) schema.maxItems else null
                )
            }
            else -> throw IllegalStateException("cannot create gene with constraints for gene:${geneClass.name}")
        }

        /*
            See:
            https://swagger.io/docs/specification/adding-examples/
            https://swagger.io/specification/
            https://swagger.io/specification/#schema-object

            TODO This is not a full handling:
            - example/examples can be defined at same level of "schema" object, ie, in a Parameter Object.
              "example" behave the same, whereas "examples" is different (here inside "schema" as an array of values,
              whereas there in a Parameter Object as an array of object definitions).
            - note that the use of "example" inside a Schema Object is deprecated, and can lead to quite a few unexpected and counter
              intuitive behavior. should be avoided.
            - technically there can be "x-example" as well (but that is mainly for older versions of the OpenAPI that
                did not support example/examples keywords as widely as now?)
         */

        val defaultValue = if(options.probUseDefault > 0) schema.default else null
        val exampleValue = if(options.probUseExamples > 0) schema.example else null
        val multiExampleValues = if(options.probUseExamples > 0) schema.examples else null

        val examples = mutableListOf<String>()
        if(exampleValue != null) {
            val raw = asRawString(exampleValue)
            examples.add(raw)
            val arrayM = if(raw.startsWith("[")) "If you are wrongly passing to it an array of values, " +
                    "the parser would read it as an array string or simply ignore it. "
            else ""
            messages.add("The use of 'example' inside a Schema Object is deprecated in OpenAPI. Rather use 'examples'." +
                     " ${arrayM}Read value: $raw")
            //TODO a problem here is that currently number arrays would be ignored, and so this message would not written.
            //however, would need to check if still the case in future in new versions of the parser
        }
        if(multiExampleValues != null && multiExampleValues.isNotEmpty()){
            //possibly bug in parser, but it was reading strings values double-quoted in this case
            examples.addAll(multiExampleValues.map { asRawString(it) })
        }
        examples.addAll( exampleObjects.map { asRawString(it) })


        val defaultGene = if(defaultValue != null){
            when{
                NumberGene::class.java.isAssignableFrom(geneClass)
                -> EnumGene("default", listOf(defaultValue.toString()),0,true)

                geneClass == StringGene::class.java
                        || geneClass == Base64StringGene::class.java
                        || geneClass == RegexGene::class.java
                -> EnumGene<String>("default", listOf(asRawString(defaultValue)),0,false)

                //TODO Arrays
                else -> {
                    messages.add("Unable to handle 'default': ${asRawString(defaultValue)}")
                    null
                }
            }
        } else null

        val exampleGene = if(examples.isNotEmpty()){
            when{
                NumberGene::class.java.isAssignableFrom(geneClass)
                -> EnumGene(EXAMPLES_NAME, examples,0,true)

                geneClass == StringGene::class.java
                        || geneClass == Base64StringGene::class.java
                        || geneClass == RegexGene::class.java
                -> EnumGene<String>(EXAMPLES_NAME, examples,0,false)

                //TODO Arrays
                else -> {
                    messages.add("Unable to handle 'examples': ${examples.joinToString(" , ")}")
                    null
                }
            }
        } else null

        return createGeneWithExampleAndDefault(exampleGene, defaultGene, mainGene, options, name)
    }

    private fun createGeneWithExampleAndDefault(
        exampleGene: Gene?,
        defaultGene: Gene?,
        mainGene: Gene,
        options: Options,
        name: String
    ): Gene {
        if (exampleGene == null && defaultGene == null) {
            //no special handling
            return mainGene
        }

        if (exampleGene != null && defaultGene != null) {
            val pd = options.probUseDefault
            val pe = options.probUseExamples
            val pm = 1 - pd - pe
            return ChoiceGene(name, listOf(defaultGene, exampleGene, mainGene), 0, listOf(pd, pe, pm))
        }

        if (exampleGene != null) {
            val pe = options.probUseExamples
            val pm = 1 - pe
            return ChoiceGene(name, listOf(exampleGene, mainGene), 0, listOf(pe, pm))
        }

        if (defaultGene != null) {
            val pd = options.probUseDefault
            val pm = 1 - pd
            return ChoiceGene(name, listOf(defaultGene, mainGene), 0, listOf(pd, pm))
        }

        throw IllegalStateException("BUG: logic error, this code should never be reached")
    }

    private fun asRawString(x : Any) : String {
        val s = x.toString()
        if(s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length - 1)
        return s
    }

    /**
     * Buils a StringGene that represents a char value.
     * Char values are modelled as string of fixed size 1.
     */
    private fun buildStringGeneForChar(
        name: String,
        isInPath: Boolean
    ): StringGene {

        return StringGene(
            name,
            minLength = 1,
            maxLength = 1,
            invalidChars = if(isInPath) listOf('/','.') else listOf()
        )
    }

    private fun buildStringGene(
        name: String,
        options: Options,
        schema: Schema<*>,
        isInPath: Boolean
    ): StringGene {

        val defaultMin = if(isInPath) 1 else 0

        return StringGene(
            name,
            maxLength = if (options.enableConstraintHandling) schema.maxLength
                ?: EMConfig.stringLengthHardLimit else EMConfig.stringLengthHardLimit,
            minLength = max(defaultMin, if (options.enableConstraintHandling) schema.minLength ?: 0 else 0),
            invalidChars = if(isInPath) listOf('/','.') else listOf()
        )
    }

    private fun createObjectFromReference(name: String,
                                          reference: String,
                                          schemaHolder: RestSchema,
                                          currentSchema: SchemaOpenAPI,
                                          history: Deque<String> = ArrayDeque(),
                                          options: Options,
                                          examples: List<Any>,
                                          messages: MutableList<String>
    ): Gene {

        /*
            The problem in caching objects is that their tree can depend on where they are mounted.
            For example, assume A->B  will not work (ie cycle) if mounted under another object that has
            B as ancestor, eg, D->C->B->X->A.
            An easy case in which this cannot happen is when the target object is a root, ie used directly
            in a parameter and not inside other objects. In such cases, we can cache it.
         */
        val isRoot = history.isEmpty()

        /*
            We need to avoid cycles like A.B.A...
            From root to leaves, how many repeated object should appear on a path?
            TODO Maybe this should be config to experiment with.
            Anyway, it is a problem in scout-api
         */
        val cycleDepth = 1

        if (history.count { it == reference } >= cycleDepth) {
            return CycleObjectGene("Cycle for: $reference")
        }

        if (isRoot && refCache.containsKey(reference)) {
            return refCache[reference]!!.copy()
        }

        /*
            TODO This could be customized in EMConfig
         */
        val depthLimit = 5
        if(history.size == depthLimit){
            return LimitObjectGene("Object-depth limit reached for: $reference")
        }

        try {
            //FIXME should not usi URI. see SchemaUtils
            URI(reference)
        } catch (e: URISyntaxException) {
            LoggingUtil.uniqueWarn(log, "Object reference is not a valid URI: $reference")
        }

        val schema = SchemaUtils.getReferenceSchema(schemaHolder,currentSchema, reference,messages)

        if (schema == null) {
            //token after last /
            val classDef = getClassDef(reference)

            LoggingUtil.uniqueWarn(log, "No $classDef among the object definitions in the OpenApi file")

            return ObjectGene(name, listOf(), classDef)
        }

        history.push(reference)

        val gene = getGene(name, schema, schemaHolder, currentSchema, history, getClassDef(reference), options,  examples = examples, messages = messages)

        if(isRoot) {
            GeneUtils.preventCycles(gene)
            GeneUtils.preventLimit(gene)
            refCache[reference] = gene
        }

        history.pop()

        return gene
    }

    private fun getClassDef(reference: String) = reference.substring(reference.lastIndexOf("/") + 1)


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
