package org.evomaster.core.output

import com.google.gson.Gson
import io.swagger.models.Swagger
import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.gene.GeneUtils
import javax.ws.rs.core.MediaType

class ExpectationsWriter {
    private var format: OutputFormat = OutputFormat.JAVA_JUNIT_4
    private val expectationsMasterSwitch = "expectationsMasterSwitch"
    private val responseStructureOracle = "responseStructureOracle"
    private lateinit var swagger: Swagger
    private lateinit var partialOracles: PartialOracles

    fun setFormat(format: OutputFormat){
        this.format = format
    }

    fun setSwagger(swagger: Swagger){
        this.swagger = swagger
    }

    fun setPartialOracles(partialOracles: PartialOracles){
        this.partialOracles = partialOracles
    }

    fun addDeclarations(lines: Lines){
        lines.addEmpty()
        when{
            format.isJava() -> lines.append("ExpectationHandler expectationHandler = expectationHandler()")
            format.isKotlin() -> lines.append("val expectationHandler: ExpectationHandler = expectationHandler()")

        }
        lines.indented {
            lines.add(".expect($expectationsMasterSwitch)")
            if (format.isJava()) lines.append(";")
        }

    }

    fun handleGenericFirstLine(call: RestCallAction, lines: Lines, res: RestCallResult, name: String, header: String){
        lines.addEmpty()
        when {
            format.isKotlin() -> lines.append("val $name: ValidatableResponse = ")
            format.isJava() -> lines.append("ValidatableResponse $name = ")
        }
        lines.append("given()$header")
    }

    fun handleGenericLastLine(call: RestCallAction, res: RestCallResult, lines: Lines, counter: Int){
        if(format.isJava()) {lines.append(";")}
        lines.deindent(2)
    }

    fun handleExpectationSpecificLines(call: RestCallAction, lines: Lines, res: RestCallResult, name: String){
        lines.addEmpty()
        when{
            format.isKotlin() -> lines.add("val json_$name: JsonPath = ")
            format.isJava() -> lines.add("JsonPath json_$name = $name")
        }

        lines.append(".extract().response().jsonPath()")
        if(format.isJava()) {lines.append(";")}
    }

    fun handleExpectations(call: RestCallAction, lines: Lines, result: RestCallResult, active: Boolean, name: String) {

        /*
        TODO: This is a WiP to show the basic idea of the expectations:
        An exception is thrown ONLY if the expectations are set to active.
        If inactive, the condition will still be processed (with a goal to later adding to summaries or
        other information processing/handling that might be needed), but it does not cause
        the test case to fail regardless of truth value.

        The example below aims to show this behaviour and provide a reminder.
        As it is still work in progress, expect quite significant changes to this.
        */

        lines.add("expectationHandler")
        lines.indented {
            lines.add(".expect()")
            addExpectationsWithoutObjects(result, lines, name)
            partialOracles.responseStructure(call, lines, result, name)
            if (format.isJava()) { lines.append(";")}
        }
    }

    private fun addExpectationsWithoutObjects(result: RestCallResult, lines: Lines, name: String) {
        if (result.getBodyType() != null) {
            // if there is a body, add expectations based on the body type. Right now only application/json is supported
            when {
                result.getBodyType()!!.isCompatible(MediaType.APPLICATION_JSON_TYPE) -> {
                    when (result.getBody()?.first()) {
                        '[' -> {
                            // This would be run if the JSON contains an array of objects
                            val resContents = Gson().fromJson(result.getBody(), ArrayList::class.java)
                            val printableTh = "numbersMatch(" +
                                    "json_$name.getJsonObject(\"size\"), " +
                                    "${resContents.size})"
                            lines.add(".that($expectationsMasterSwitch, ($printableTh))")
                            //TODO: individual objects in this collection also need handling
                            resContents.forEachIndexed { index, result ->
                                val fieldName = "get($index)"
                                val printableElement = handleFieldValuesExpect(name, fieldName, result)
                                if (printableElement != "null"
                                        && printableElement != TestCaseWriter.NOT_COVERED_YET
                                        && !printableTh.contains("logged")
                                ) {
                                    lines.add(".that($expectationsMasterSwitch, $printableElement)")
                                }
                            }

                        }
                        '{' -> {
                            // This would be run if the JSON contains a single object
                            val resContents = Gson().fromJson(result.getBody(), Object::class.java)

                            (resContents as Map<*, *>).keys
                                    .filter { !it.toString().contains("timestamp") && !it.toString().contains("cache") }
                                    .forEach {
                                        val printableTh = handleFieldValuesExpect(name, it.toString(), resContents[it])
                                        if (printableTh != "null"
                                                && printableTh != TestCaseWriter.NOT_COVERED_YET
                                                && !printableTh.contains("logged") //again, unpleasant, but IDs logged as part of the error message are a problem
                                            //TODO: find a more elegant way to deal with IDs, object refs, timestamps, etc.
                                        ) {
                                            lines.add(".that($expectationsMasterSwitch, $printableTh)")
                                        }
                                    }
                        }
                        else -> {
                            // this shouldn't be run if the JSON is okay. Panic! Update: could also be null. Pause, then panic!
                            if(result.getBody() != null) lines.add(".that($expectationsMasterSwitch, subStringsMatch($name.extract().response().asString(), \"${GeneUtils.applyEscapes(result.getBody().toString(), mode = GeneUtils.EscapeMode.ASSERTION, format = format)}\"))")
                                //lines.add(".that($expectationsMasterSwitch, json_$name.jsonParser.json == null)")
                                //lines.add(".that($expectationsMasterSwitch, stringsMatch(json_$name.toString(), \"${GeneUtils.applyEscapes(result.getBody().toString(), mode = GeneUtils.EscapeMode.ASSERTION, format = format)}\"))")
                                //lines.add(".body(containsString(\"${GeneUtils.applyEscapes(result.getBody().toString(), mode = GeneUtils.EscapeMode.ASSERTION, format = format)}\"))")
                            else lines.add(".that($expectationsMasterSwitch, json_$name.toString().isEmpty())")
                            //else lines.add(".body(isEmptyOrNullString())")
                        }
                    }
                }
                result.getBodyType()!!.isCompatible(MediaType.TEXT_PLAIN_TYPE) -> {
                    if(result.getBody() != null) lines.add(".that($expectationsMasterSwitch, subStringsMatch($name.extract().response().asString(), \"${GeneUtils.applyEscapes(result.getBody().toString(), mode = GeneUtils.EscapeMode.ASSERTION, format = format)}\"))")
                        //lines.add(".body(containsString(\"${GeneUtils.applyEscapes(result.getBody().toString(), mode = GeneUtils.EscapeMode.ASSERTION, format = format)}\"))")
                    else lines.add(".that($expectationsMasterSwitch, json_$name.toString().isEmpty())")
                        //lines.add(".body(isEmptyOrNullString())")
                }
            }
        }
    }

    private fun handleFieldValuesExpect(objectName: String, fieldName: String, resContentsItem: Any?): String{
        if (resContentsItem == null) {
            return TestCaseWriter.NOT_COVERED_YET
        }
        else{
            when(resContentsItem::class) {
                Double::class -> return "numbersMatch(json_$objectName.getJsonObject(\"$fieldName\")," +
                        " ${resContentsItem as Double})"
                String::class -> return "subStringsMatch(json_$objectName.getJsonObject(\"$fieldName\")," +
                        "\"${GeneUtils.applyEscapes((resContentsItem as String), mode = GeneUtils.EscapeMode.EXPECTATION, format = format)}\")"
                else -> return TestCaseWriter.NOT_COVERED_YET
            }
        }
    }

}