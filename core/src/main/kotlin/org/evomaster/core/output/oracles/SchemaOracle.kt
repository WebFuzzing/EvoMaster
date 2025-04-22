package org.evomaster.core.output.oracles


/**
 * The [SchemaOracle] class generates expectations and writes them to the code.
 *
 * A check is made to see if the structure of te response matches a structure in the schema.
 * If a response is successful and returns an object.
 *
 * The [SchemaOracle]
 * checks that the returned object is of the appropriate type and structure (not content). I.e., the method
 * checks that the returned object has all the compulsory field that the type has (according to the
 * swagger definition) and that all the optional fields present are included in the definition.
 */

@Deprecated("No longer used")
class SchemaOracle : ImplementedOracle() {
//    private val variableName = "rso"
//    private lateinit var objectGenerator: ObjectGenerator
//    private val basicTypes = arrayOf("string", "number", "integer", "boolean", "array", "object")
//
//    companion object {
//        private val log: Logger = LoggerFactory.getLogger(SchemaOracle::class.java)
//    }
//
//    override fun variableDeclaration(lines: Lines, format: OutputFormat) {
//        lines.add("/**")
//        lines.add("* $variableName - response structure oracle - checking that the response objects match the responses defined in the schema")
//        lines.add("*/")
//        when{
//            format.isJava() -> {
//                lines.add("private static boolean $variableName = false;")
//            }
//            format.isKotlin() -> {
//                lines.add("private val $variableName = false")
//            }
//        }
//    }
//
//    override fun addExpectations(call: RestCallAction, lines: Lines, res: HttpWsCallResult, name: String, format: OutputFormat) {
//        if (res.failedCall()
//                || res.getStatusCode() == 500
//                || !generatesExpectation(call, res)
//                || !format.isJavaOrKotlin()) {
//            return
//        }
//
//        val supportedRes = getSupportedResponse(call)
//        val bodyString = res.getBody()
//        when (bodyString?.first()) {
//            '[' -> {
//                // TODO: Handle arrays of objects
//                val responseObject = Gson().fromJson(bodyString, ArrayList::class.java)
//                val supportedObjs = getSupportedResponse(call)
//                val expectedObject = supportedObjs.get("${res.getStatusCode()}")
//                when {
//                    responseObject.isNullOrEmpty() -> return
//                    else -> {
//                        responseObject
//                            .forEachIndexed { index, obj ->
//                                val json_ref = when {
//                                    format.isKotlin() -> "$name.extract().response().jsonPath().getJsonObject<Any>(\"\") as ArrayList<*>"
//                                    format.isJava() -> "(List) $name.extract().response().jsonPath().getJsonObject(\"\")"
//                                    else -> return
//                                        //throw IllegalArgumentException("Expectations are currently only supported for Java and Kotlin")
//                                }
//                                val moreRef = "($json_ref).get($index)"
//                                writeExpectation(call, lines, moreRef, format, expectedObject)
//                        }
//                    }
//                }
//            }
//            '{' -> {
//                // TODO: Handle individual objects
//                val supportedObjs = getSupportedResponse(call)
//                val expectedObject = supportedObjs.get("${res.getStatusCode()}")
//
//                when {
//                    expectedObject.isNullOrEmpty() -> return //No expectations can be made (possibly another fault exists).
//                    expectedObject.equals("string", ignoreCase = true) -> return
//                    // handling single values appears to be a known problem with RestAssured and Groovy
//                    // see https://github.com/rest-assured/rest-assured/issues/949
//                    else -> {
//                        val json_ref = when {
//                            format.isKotlin() -> "$name.extract().response().jsonPath().getJsonObject<Any>(\"\")"
//                            format.isJava() -> "$name.extract().response().jsonPath().getJsonObject(\"\")"
//                            else -> return
//                                //throw IllegalArgumentException("Expectations are currently only supported for Java and Kotlin")
//                        }
//
//                        writeExpectation(call, lines, json_ref, format, expectedObject)
//                    }
//                }
//            }
//        }
//    }
//
//    fun writeExpectation(call: RestCallAction, lines: Lines,  json_ref: String, format: OutputFormat, expectedObject: String?){
//        // if the contents are objects with a ref in the schema
//        //val json_ref = "$name.extract().response().jsonPath()"
//        //val referenceObject = objectGenerator.getNamedReference("$expectedObject")
//        val referenceObject = getReferenceObject(expectedObject)
//
//        if(referenceObject.fields.isEmpty()){
//            //this could happen when schema generated automatically and it is invalid
//            return
//        }
//
//        val referenceKeys = referenceObject.fields
//                .filterNot { it is OptionalGene }
//                .map { "\"${it.name}\"" }
//                .joinToString(separator = ", ")
//
//        //this differs between kotlin and java
//        when{
//            format.isJava() ->lines.add(".that($variableName, ((Map) $json_ref).keySet().containsAll(Arrays.asList($referenceKeys)))")
//            format.isKotlin() -> lines.add(".that($variableName, ($json_ref as Map<*,*>).keys.containsAll(Arrays.asList($referenceKeys)))")
//        }
//
//        val referenceOptionalKeys = referenceObject.fields
//                .filter { it is OptionalGene }
//                .map { "\"${it.name}\"" }
//                .joinToString(separator = ", ")
//
//        /*
//         * TODO Andrea: this is unclear...
//         * why must a response contain optional fields? if those are optional, they could be missing...
//         *
//         * TODO would it better to check if there is any field that is not declared in the schema?
//         *
//        when {
//            format.isJava() -> {
//                lines.add(".that($variableName, Arrays.asList($referenceOptionalKeys)")
//                lines.add(".containsAll(((Map) $json_ref).keySet()))")
//            }
//            format.isKotlin() -> {
//                lines.add(".that($variableName, listOf<Any>($referenceOptionalKeys)")
//                lines.add(".containsAll(($json_ref as Map<*,*>).keys))")
//            }
//        }
//         */
//    }
//
//    /**
//     * The [supportedObject] function evaluates if the [obj] ObjectGene object is supported
//     * by the [call] RestCallAction. In this case, the [obj] object is obtained from
//     * the [ObjectGenerator], and therefore it is of type [ObjectGene].
//     *
//     * This requires that the [ObjectGenerator] object is set (and connected to the \
//     * OpenAPI schema, in order to get the objects potentially supported by the call.
//     *
//     */
//
//    fun supportedObject(obj: ObjectGene, call: RestCallAction): Boolean{
//        val actualKeys = obj.fields
//                .filterNot { it is OptionalGene }
//                .map { it.name }
//                .toMutableSet()
//
//        val actualOptionalKeys = obj.fields
//                .filter { it is OptionalGene }
//                .map { it.name }
//                .toMutableSet()
//
//        return supportStructure(actualKeys, actualOptionalKeys, call)
//    }
//
//    /**
//     * The [supportedObject] function evaluates if the [obj] object is supported
//     * by the [call] RestCallAction. In this case, the [obj] object is obtained from
//     * a response (e.g. by means of JSON) and therefore it's a Map, rather than
//     * obtained from the [ObjectGenerator].
//     *
//     * This requires that the [ObjectGenerator] object is set (and connected to the \
//     * OpenAPI schema, in order to get the objects potentially supported by the call.
//     *
//     */
//
//    fun supportedObject(obj: Map<*,*>, call: RestCallAction): Boolean{
//        val actualKeys = obj.keys
//                .filterNot { it is OptionalGene }
//                .map { it.toString() }
//                .toMutableSet()
//
//        val actualOptionalKeys = obj.keys
//                .filter { it is OptionalGene }
//                .map { it.toString() }
//                .toMutableSet()
//
//        return supportStructure(actualKeys, actualOptionalKeys, call)
//    }
//
//    private fun getReferenceObject(expectedKey: String?): ObjectGene{
//
//        val refObject =  when {
//            expectedKey == null -> ObjectGene("default", listOf())
//            objectGenerator.containsKey(expectedKey) -> {
//                objectGenerator.getNamedReference(expectedKey)
//            }
//            // One might find objects that are not supported.
//            // an example: EscapeRest method trickyJson returns a HashMap that is neither explicitly nor implicitly supported.
//            else -> ObjectGene("default", listOf())
//        }
//        return refObject
//    }
//
//    fun supportStructure(actualKeys: MutableSet<String>, actualOptionalKeys: MutableSet<String>, call: RestCallAction): Boolean {
//
//        val supportedObjects = getSupportedResponse(call)
//
//        return supportedObjects.any { o ->
//            val refObject = getReferenceObject(o.value)
//            val refKeys = refObject.fields
//                    .map { it.name }
//                    .toMutableSet()
//            val refCompulsoryKeys = refObject.fields
//                    .filterNot { it is OptionalGene }
//                    .map { it.name }
//                    .toMutableSet()
//
//            val compulsoryMatch = refKeys.containsAll(actualKeys) && actualKeys.containsAll(refCompulsoryKeys)
//
//            val refOptionalKeys = refObject.fields
//                    .filter { it is OptionalGene }
//                    .map { it.name }
//                    .toMutableSet()
//
//            val optionalMatch = refOptionalKeys.containsAll(actualOptionalKeys)
//
//            return compulsoryMatch && optionalMatch
//        }
//    }
//
//    /*
//    fun matchesStructure(call: RestCallAction, res: HttpWsCallResult): Boolean{
//        val supportedTypes = getSupportedResponse(call)
//        val actualType = res.getBody()
//        return false
//    }*/
//
//    /**
//     * The function [getSupportedResponse] collects the supported responses for a particular call
//     * for all the supported HTTP verbs. The response contains the object names, as identified
//     * in the references defined by the OpenAPI.
//     */
//
//    fun getSupportedResponse(call: RestCallAction): MutableMap<String, String>{
//        val verb = call.verb
//        val path = retrievePath(objectGenerator, call)
//        val specificPath = when(verb){
//            HttpVerb.GET -> path?.get
//            HttpVerb.POST -> path?.post
//            HttpVerb.PUT -> path?.put
//            HttpVerb.DELETE -> path?.delete
//            HttpVerb.PATCH -> path?.patch
//            HttpVerb.HEAD -> path?.head
//            HttpVerb.OPTIONS -> path?.options
//            HttpVerb.TRACE -> path?.trace
//            else -> null
//        }
//        val mapResponses = mutableMapOf<String, String>()
//        specificPath?.responses?.forEach { key, value ->
//            value.content?.values?.forEach { cva ->
//                //TODO: BMR the schema may need additions here
//                val valueSchema = cva.schema
//                if(valueSchema != null) {
//                    val rez = when (valueSchema) {
//                        // valueSchema.items might be null with cyclostron sut
//                        is ArraySchema -> valueSchema.items?.`$ref` ?: valueSchema.items?.type
//                        ?: "".also {
//                            /*
//                            with cyclotron sut, a response of get /data/{key}/data is specified as
//                            "responses": {
//                              "200": {
//                                "description": "The data array for a Data Bucket",
//                                "schema": {
//                                  "type": "array"
//                                }
//                              },
//                         */
//                            log.warn("missing type of a response with Array schema {}", call.getName())
//                        }
//                        is MapSchema -> {
//                            when (cva.schema.additionalProperties) {
//                                is StringSchema -> (cva.schema.additionalProperties as StringSchema).type
//                                is ObjectSchema -> (cva.schema.additionalProperties as ObjectSchema).type
//                                else -> (cva.schema.additionalProperties as Schema<*>).`$ref` ?: ""
//                            }
//                        }
//                        is StringSchema -> valueSchema.type ?: ""
//                        is IntegerSchema -> valueSchema.format ?: ""
//                        is ObjectSchema -> ""
//                        else -> valueSchema.`$ref` ?: ""
//                    }
//                    mapResponses[key] = rez!!.split("/").last()
//                } else {
//                    LoggingUtil.uniqueWarn(log, "Missing schema definition for response in ${call.path}")
//                }
//            }
//        }
//
//        return mapResponses
//    }
//
//    override fun setObjectGenerator(gen: ObjectGenerator){
//        objectGenerator = gen
//    }
//
//    override fun generatesExpectation(call: RestCallAction, res: HttpWsCallResult): Boolean {
//        // A check should be made if this should be the case (i.e. if (any of) the object(s) contained break the schema.
//        //return !(res.failedCall() || res.getStatusCode() == 500)
//        if(!::objectGenerator.isInitialized) return false
//        val supportedObjs = getSupportedResponse(call)
//        val expectedObject = supportedObjs.get("${res.getStatusCode()}") ?: return false
//
//        // Assess if the expected object is defined by the OpenAPI or if it's a basic type
//        if(!objectGenerator.containsKey(expectedObject)
//                &&
//                !basicTypes.contains(expectedObject)) {
//            return true
//        }
//        // If the expected object is of a basic type, no additional expectations are made on its structure.
//        if(basicTypes.contains(expectedObject)) return false
//        var supported = true
//
//        if (res.getBodyType()?.isCompatible(MediaType.APPLICATION_JSON_TYPE) == true){
//            val actualObject = try {
//                Gson().fromJson(res.getBody(), Object::class.java)
//            } catch (e: JsonSyntaxException) {
//                return false
//            }
//
//            if  (actualObject is Map<*,*>)
//                supported = supportedObject(actualObject, call)
//            else if (actualObject is List<*>
//                    && (actualObject as List<*>).isNotEmpty()
//                    && (actualObject as List<*>).first() is Map<*,*>){
//                supported = supportedObject((actualObject as List<*>).first() as Map<*, *>, call)
//            }
//            // A call should generate an expectation if:
//            // The return object differs in structure from the expected (i.e. swagger object).
//            // The return type is different than the actual type (i.e. return type is not supported)
//        }
//
//        return !supported
//    }
//
//    override fun generatesExpectation(individual: EvaluatedIndividual<*>): Boolean {
//        // A check should be made if this should be the case (i.e. if (any of) the object(s) contained break the schema.
//        //return !(res.failedCall() || res.getStatusCode() == 500)
//        if(individual.individual !is RestIndividual) return false
//
//        if(!::objectGenerator.isInitialized) return false
//
//        return individual.evaluatedMainActions().any {
//            val call = it.action as RestCallAction
//            val res = it.result as HttpWsCallResult
//            generatesExpectation(call, res)
//            /*val supportedObjs = getSupportedResponse(call)
//            val expectedObject = supportedObjs.get("${res.getStatusCode()}") ?: return false
//            if(!objectGenerator.containsKey(expectedObject)) return false
//            val referenceObject = objectGenerator.getNamedReference(expectedObject)
//            !supportedObject(referenceObject, call)*/
//        }
//    }
//
//    override fun selectForClustering(action: EvaluatedAction): Boolean {
//        if (action.action is RestCallAction
//                && action.result is HttpWsCallResult
//                && !(action.action as RestCallAction).skipOracleChecks){
//            return generatesExpectation(action.action as RestCallAction, action.result as HttpWsCallResult)
//        }
//        else return false
//    }
//
//    override fun getName():String {
//        return "SchemaOracle"
//    }
//
//    override fun adjustName(): String?{
//        return "_apiSchemaMismatch"
//    }
}