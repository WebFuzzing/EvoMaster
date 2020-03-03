package org.evomaster.core.output.oracles

import com.google.gson.Gson
import io.swagger.v3.oas.models.media.*
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.ObjectGenerator
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.EvaluatedAction
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.OptionalGene


/**
 * The [SchemaOracle] class generates expectations and writes them to the code.
 *
 * A check is made to see if the structure of te response matches a structure in the schema.
 * If a response is successful and returns an object. The [SchemaOracle]
 * checks that the returned object is of the appropriate type and structure (not content). I.e., the method
 * checks that the returned object has all the compulsory field that the type has (according to the
 * swagger definition) and that all the optional fields present are included in the definition.
 */


class SchemaOracle : ImplementedOracle() {
    private val variableName = "rso"
    private lateinit var objectGenerator: ObjectGenerator

    override fun variableDeclaration(lines: Lines, format: OutputFormat) {
        lines.add("/**")
        lines.add("* $variableName - response structure oracle - checking that the response objects match the responses defined in the schema")
        lines.add("*/")
        when{
            format.isJava() -> {
                lines.add("private static boolean $variableName = false;")
            }
            format.isKotlin() -> {
                lines.add("private val $variableName = false")
            }
        }
    }

    override fun addExpectations(call: RestCallAction, lines: Lines, res: RestCallResult, name: String, format: OutputFormat) {
        if (res.failedCall()
                || res.getStatusCode() == 500) {
            return
        }

        try {
            getSupportedResponse(call)
        }
        catch (e: Exception){

        }

        val bodyString = res.getBody()
        when (bodyString?.first()) {
            '[' -> {
                // TODO: Handle arrays of objects
                val responseObject = Gson().fromJson(bodyString, ArrayList::class.java)
            }
            '{' -> {
                // TODO: Handle individual objects
                val supportedObjs = getSupportedResponse(call)
                val expectedObject = supportedObjs.get("${res.getStatusCode()}")
                when {
                    expectedObject.isNullOrEmpty() -> return //No expectations can be made (possibly another fault exists).
                    expectedObject.equals("string", ignoreCase = true) -> return
                    // handling single values appears to be a known problem with RestAssured and Groovy
                    // see https://github.com/rest-assured/rest-assured/issues/949
                    else -> {
                        // if the contents are objects with a ref in the schema
                        val referenceObject = objectGenerator.getNamedReference("$expectedObject")
                        val isItThough = supportedObject(referenceObject, call)

                        val referenceKeys = referenceObject.fields
                                .filterNot { it is OptionalGene }
                                .map { "\"${it.name}\"" }
                                .joinToString(separator = ", ")

                        //this differs between kotlin and java
                        when{
                            format.isJava() ->lines.add(".that($variableName, json_$name.getMap(\"\").keySet().containsAll(Arrays.asList($referenceKeys)))")
                            format.isKotlin() -> lines.add(".that($variableName, json_$name.getMap<Any, Any>(\"\").keys.containsAll(Arrays.asList($referenceKeys)))")
                        }
                        val referenceOptionalKeys = referenceObject.fields
                                .filter { it is OptionalGene }
                                .map { "\"${it.name}\"" }
                                .joinToString(separator = ", ")

                        when {
                            format.isJava() -> lines.add(".that($variableName, Arrays.asList($referenceOptionalKeys).containsAll(json_$name.getMap(\"\").keySet()))")
                            format.isKotlin() -> lines.add(".that($variableName, listOf<Any>($referenceOptionalKeys).containsAll(json_$name.getMap<Any, Any>(\"\").keys))")
                        }
                    }
                }
            }
        }
    }


    fun supportedObject(obj: ObjectGene, call: RestCallAction): Boolean{
        val supportedObjects = getSupportedResponse(call)
        return supportedObjects.any { o ->
            val refObject = objectGenerator.getNamedReference(o.value)
            val refKeys = refObject.fields
                    .filterNot { it is OptionalGene }
                    .map { it.name }
                    .toMutableSet()
            val actualKeys = obj.fields
                    .filterNot { it is OptionalGene }
                    .map { it.name }
                    .toMutableSet()

            val compulsoryMatch = refKeys.containsAll(actualKeys) && actualKeys.containsAll(refKeys)

            val refOptionalKeys = refObject.fields
                    .filter { it is OptionalGene }
                    .map { it.name }
                    .toMutableSet()

            val actualOptionalKeys = obj.fields
                    .filter { it is OptionalGene }
                    .map { it.name }
                    .toMutableSet()

            val optionalMatch = refOptionalKeys.containsAll(actualOptionalKeys)

            return compulsoryMatch && optionalMatch
        }
    }

    fun matchesStructure(call: RestCallAction, res: RestCallResult): Boolean{
        val supportedTypes = getSupportedResponse(call)
        val actualType = res.getBody()
        return false
    }

    fun getSupportedResponse(call: RestCallAction): MutableMap<String, String>{
        val verb = call.verb
        val path = objectGenerator.getSwagger().paths.get(call.path.toString())
        val specificPath = when(verb){
            HttpVerb.GET -> path?.get
            HttpVerb.POST -> path?.post
            HttpVerb.PUT -> path?.put
            HttpVerb.DELETE -> path?.delete
            HttpVerb.PATCH -> path?.patch
            HttpVerb.HEAD -> path?.head
            HttpVerb.OPTIONS -> path?.options
            HttpVerb.TRACE -> path?.trace
            else -> null
        }
        val mapResponses = mutableMapOf<String, String>()
        specificPath?.responses?.forEach { key, value ->
            value.content.values.map { cva ->
                val valueSchema = cva.schema
                val rez = when (valueSchema) {
                    is ArraySchema -> valueSchema.items.`$ref` ?: valueSchema.items.type
                    is MapSchema -> (cva.schema.additionalProperties as StringSchema).type ?: (cva.schema.additionalProperties as Schema<*>).`$ref`
                    is StringSchema -> valueSchema.type
                    is IntegerSchema -> valueSchema.format
                    is ObjectSchema -> ""
                    is Schema -> valueSchema.`$ref`
                    else -> ""
                }
                mapResponses.put(key, rez.split("/").last())
            }
        }

        return mapResponses
    }

    override fun setObjectGenerator(gen: ObjectGenerator){
        objectGenerator = gen
    }

    override fun generatesExpectation(call: RestCallAction, lines: Lines, res: RestCallResult, name: String, format: OutputFormat): Boolean {
        // A check should be made if this should be the case (i.e. if (any of) the object(s) contained break the schema.
        //return !(res.failedCall() || res.getStatusCode() == 500)
        return true
    }

    override fun selectForClustering(action: EvaluatedAction): Boolean {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return false
    }
}