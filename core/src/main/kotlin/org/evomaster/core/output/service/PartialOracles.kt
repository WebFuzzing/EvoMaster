package org.evomaster.core.output.service

import com.google.gson.Gson
import org.evomaster.core.output.Lines
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.OptionalGene


class PartialOracles {
    private lateinit var objectGenerator: ObjectGenerator

    /**
     * The [responseStructure] method. If a response is successful and returns an object. The [responseStructure]
     * method checks that the returned object is of the appropriate type and structure (not content). I.e., the method
     * checks that the returned object has all the compulsory field that the type has (according to the
     * swagger definition) and that all the optional fields present are included in the definition.
     */
    fun setGenerator(objGen: ObjectGenerator){
        objectGenerator = objGen
    }
    fun responseStructure(call: RestCallAction, lines: Lines, res: RestCallResult, name: String){
        if (res.failedCall()) return
        val oracleName = "responseStructureOracle"
        val bodyString = res.getBody()
        when (bodyString?.first()) {
            '[' -> {
                // TODO: Handle arrays of objects
                val responseObject = Gson().fromJson(bodyString, ArrayList::class.java)
            }
            '{' -> {
                // TODO: Handle individual objects
                val responseObject = Gson().fromJson(bodyString, Map::class.java)
                call.responseRefs.forEach{
                    val referenceObject = objectGenerator.getNamedReference(it)
                    //Expect that the response has all the compulsory (i.e. non-optional) fields
                    // responseObject.keys.containsAll(referenceObject.fields.filterNot{ it is OptionalGene }.map { it.name })
                    //.that(expectationsMasterSwitch, json_call_0.getMap("").keySet().containsAll(Arrays.asList("id", "uri", "name")))
                    val referenceKeys = referenceObject.fields
                            .filterNot { it is OptionalGene }
                            .map { "\"${it.name}\"" }
                            .joinToString(separator = ", ")

                    lines.add(".that($oracleName, json_$name.getMap(\"\").keySet().containsAll(Arrays.asList($referenceKeys)))")


                    //Expect that the reference contains all the optional field in the response
                    //referenceObject.fields.filter{ it is OptionalGene}.map { it.name }.containsAll(responseObject.keys)
                    val referenceOptionalKeys = referenceObject.fields
                            .filter { it is OptionalGene }
                            .map { "\"${it.name}\"" }
                            .joinToString(separator = ", ")
                    lines.add(".that($oracleName, Arrays.asList($referenceOptionalKeys).containsAll(json_$name.getMap(\"\").keySet()))")
                }
            }
        }
    }
}