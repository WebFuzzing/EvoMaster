package org.evomaster.core.output.oracles

import com.google.gson.Gson
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.service.ObjectGenerator
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.gene.OptionalGene


/**
 * The [ResponseStructureOracle] class generates expectations and writes them to the code.
 *
 * A check is made to see if the structure of te response matches a structure in the schema.
 * If a response is successful and returns an object. The [ResponseStructureOracle]
 * checks that the returned object is of the appropriate type and structure (not content). I.e., the method
 * checks that the returned object has all the compulsory field that the type has (according to the
 * swagger definition) and that all the optional fields present are included in the definition.
 */


class ResponseStructureOracle : ImplementedOracle() {
    private val variableName = "rso"
    private lateinit var objectGenerator: ObjectGenerator

    override fun variableDeclaration(lines: Lines, format: OutputFormat) {
        lines.add("// $variableName - response structure oracle - checking that the response objects match the responses defined in the schema")
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
        val bodyString = res.getBody()
        when (bodyString?.first()) {
            '[' -> {
                // TODO: Handle arrays of objects
                val responseObject = Gson().fromJson(bodyString, ArrayList::class.java)
            }
            '{' -> {
                // TODO: Handle individual objects
                call.responseRefs.forEach{
                    if (res.getStatusCode().toString() != it.key) return@forEach
                    val referenceObject = objectGenerator.getNamedReference(it.value)
                    //Expect that the response has all the compulsory (i.e. non-optional) fields

                    val referenceKeys = referenceObject.fields
                            .filterNot { it is OptionalGene }
                            .map { "\"${it.name}\"" }
                            .joinToString(separator = ", ")

                    //this differs between kotlin and java
                    when{
                        format.isJava() ->lines.add(".that($variableName, json_$name.getMap(\"\").keySet().containsAll(Arrays.asList($referenceKeys)))")
                        format.isKotlin() -> lines.add(".that($variableName, json_$name.getMap<Any, Any>(\"\").keys.containsAll(Arrays.asList($referenceKeys)))")
                    }



                    //Expect that the reference contains all the optional field in the response
                    //referenceObject.fields.filter{ it is OptionalGene}.map { it.name }.containsAll(responseObject.keys)
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

    override fun setObjectGenerator(gen: ObjectGenerator){
        objectGenerator = gen
    }

    override fun generatesExpectation(call: RestCallAction, lines: Lines, res: RestCallResult, name: String, format: OutputFormat): Boolean {
        return !(res.failedCall() || res.getStatusCode() == 500)
    }
}