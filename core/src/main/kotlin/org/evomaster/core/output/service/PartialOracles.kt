package org.evomaster.core.output.service

import com.google.gson.Gson
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.OptionalGene

/**
 * [PartialOracles] are meant to be a way to handle different types of soft assertions/expectations (name may change in future)
 *
 * The idea is that any new type of expectation is a partial oracle (again, name may change, if anything more appropriate
 * emerges). For example, if a return object is specified in the Swagger for a given endpoint with a given status code,
 * then the object returned should have the same structure as the Swagger reference (see [responseStructure] below.
 *
 * They are "partial" because failing such a test is not necessarily indicative of a bug, as it could be some sort
 * or shortcut or something (and since REST semantics are not strictly enforced, it cannot be an assert). Nevertheless,
 * it would be a break with expected semantics and could be indicative of a fault or design problem.
 *
 * The [PartialOracles] would (in future) each receive their own variable to turn on or off, and only those selected
 * would be added to the code. So they should be independent from each other. The idea is that, either during generation
 * or during execution, the user can decide if certain partial oracles are of interest at the moment, and turn then
 * on or off as required.
 *
 */

class PartialOracles {
    private lateinit var objectGenerator: ObjectGenerator
    private lateinit var format: OutputFormat

    /**
     * The [responseStructure] method. If a response is successful and returns an object. The [responseStructure]
     * method checks that the returned object is of the appropriate type and structure (not content). I.e., the method
     * checks that the returned object has all the compulsory field that the type has (according to the
     * swagger definition) and that all the optional fields present are included in the definition.
     */
    fun setGenerator(objGen: ObjectGenerator){
        objectGenerator = objGen
    }
    fun setFormat(format: OutputFormat = OutputFormat.KOTLIN_JUNIT_5){
        this.format = format
    }

    fun responseStructure(call: RestCallAction, lines: Lines, res: RestCallResult, name: String){
        if (res.failedCall()
                || res.getStatusCode() == 500) {
            return
        }
        val oracleName = "responseStructureOracle"
        val bodyString = res.getBody()
        when (bodyString?.first()) {
            '[' -> {
                // TODO: Handle arrays of objects
                val responseObject = Gson().fromJson(bodyString, ArrayList::class.java)
            }
            '{' -> {
                // TODO: Handle individual objects
                //val responseObject = Gson().fromJson(bodyString, Map::class.java)
                call.responseRefs.forEach{
                    if (res.getStatusCode().toString() != it.key) return@forEach
                    val referenceObject = objectGenerator.getNamedReference(it.value)
                    //Expect that the response has all the compulsory (i.e. non-optional) fields
                    // responseObject.keys.containsAll(referenceObject.fields.filterNot{ it is OptionalGene }.map { it.name })
                    //.that(expectationsMasterSwitch, json_call_0.getMap("").keySet().containsAll(Arrays.asList("id", "uri", "name")))

                    val referenceKeys = referenceObject.fields
                            .filterNot { it is OptionalGene }
                            .map { "\"${it.name}\"" }
                            .joinToString(separator = ", ")

                    //this differs between kotlin and java
                    when{
                        format.isJava() ->lines.add(".that($oracleName, json_$name.getMap(\"\").keySet().containsAll(Arrays.asList($referenceKeys)))")
                        format.isKotlin() -> lines.add(".that($oracleName, json_$name.getMap<Any, Any>(\"\").keys.containsAll(Arrays.asList($referenceKeys)))")
                    }



                    //Expect that the reference contains all the optional field in the response
                    //referenceObject.fields.filter{ it is OptionalGene}.map { it.name }.containsAll(responseObject.keys)
                    val referenceOptionalKeys = referenceObject.fields
                            .filter { it is OptionalGene }
                            .map { "\"${it.name}\"" }
                            .joinToString(separator = ", ")

                    when {
                        format.isJava() -> lines.add(".that($oracleName, Arrays.asList($referenceOptionalKeys).containsAll(json_$name.getMap(\"\").keySet()))")
                        format.isKotlin() -> lines.add(".that($oracleName, listOf<Any>($referenceOptionalKeys).containsAll(json_$name.getMap<Any, Any>(\"\").keys))")
                    }

                }
            }
        }
    }
}