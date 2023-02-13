package org.evomaster.core.output.service

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionResult
import org.evomaster.core.output.CookieWriter
import org.evomaster.core.output.Lines
import org.evomaster.core.output.SqlWriter
import org.evomaster.core.output.TokenWriter
import org.evomaster.core.search.EvaluatedDbAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.utils.GeneUtils

abstract class ApiTestCaseWriter : TestCaseWriter() {

    protected fun createUniqueResponseVariableName(): String {
        val name = "res_$counter"
        counter++
        return name
    }

    protected fun createUniqueBodyVariableName(): String {
        val name = "body_$counter"
        counter++
        return name
    }

    override fun handleFieldDeclarations(lines: Lines, baseUrlOfSut: String, ind: EvaluatedIndividual<*>, insertionVars: MutableList<Pair<String, String>>) {

        //FIXME this is getting auth, not field declaration
        CookieWriter.handleGettingCookies(format, ind, lines, baseUrlOfSut, this)
        TokenWriter.handleGettingTokens(format, ind, lines, baseUrlOfSut, this)

        //FIXME this doing initializations, not field declaration
        val initializingActions = ind.individual.seeInitializingActions().filterIsInstance<DbAction>()
        val initializingActionResults = (ind.seeResults(initializingActions))
        if (initializingActionResults.any { (it as? DbActionResult) == null })
            throw IllegalStateException("the type of results are expected as DbActionResults")


        if (ind.individual.seeInitializingActions().isNotEmpty()) {
            SqlWriter.handleDbInitialization(
                    format,
                    initializingActions.indices.map {
                        EvaluatedDbAction(initializingActions[it], initializingActionResults[it] as DbActionResult)
                    },
                    lines, insertionVars = insertionVars, skipFailure = config.skipFailureSQLInTestFile)
        }
    }

    /**
     * handle assertion with text plain
     */
    fun handleTextPlainTextAssertion(bodyString: String?, lines: Lines, bodyVarName: String?) {

        if (bodyString.isNullOrBlank()) {
            lines.add(emptyBodyCheck(bodyVarName))
        } else {
            //TODO in the call above BODY was used... what's difference from TEXT?
            lines.add(bodyIsString(bodyString, GeneUtils.EscapeMode.TEXT, bodyVarName))
        }
    }

    /**
     * handle assertion with json body string
     */
    fun handleJsonStringAssertion(bodyString: String?, lines: Lines, bodyVarName: String?, isTooLargeBody: Boolean) {
        when (bodyString?.trim()?.first()) {
            //TODO this should be handled recursively, and not ad-hoc here...
            '[' -> {
                try{
                // This would be run if the JSON contains an array of objects.
                val list = Gson().fromJson(bodyString, List::class.java)
                handleAssertionsOnList(list, lines, "", bodyVarName)
                } catch (e: JsonSyntaxException) {
                    lines.add("/* Failed to parse JSON response */")
                }
            }
            '{' -> {
                // JSON contains an object
                try {
                    val resContents = Gson().fromJson(bodyString, Map::class.java)
                    handleAssertionsOnObject(resContents as Map<String, *>, lines, "", bodyVarName)
                } catch (e: JsonSyntaxException) {
                    lines.add("/* Failed to parse JSON response */")
                }
            }
            '"' -> {
                lines.add(bodyIsString(bodyString, GeneUtils.EscapeMode.BODY, bodyVarName))
            }
            else -> {
                /*
                    This branch will be called if the JSON is null (or has a basic type)
                    Currently, it converts the contents to String.

                    TODO do we have tests for it? and if it is true for RestAssured, anyway
                    it does not the seem the case for Jest/SuperAgent
                 */
                when {
                    isTooLargeBody -> lines.add("/* very large body, which was not handled during the search */")

                    bodyString.isNullOrBlank() -> lines.add(emptyBodyCheck(bodyVarName))

                    else -> handlePrimitive(lines, bodyString, "", bodyVarName)
                }
            }
        }
    }


    private fun handlePrimitive(lines: Lines, bodyString: String, fieldPath: String, responseVariableName: String?) {

        val s = bodyString.trim()

        when {
            format.isJavaOrKotlin() -> {
                lines.add(bodyIsString(s, GeneUtils.EscapeMode.BODY, responseVariableName))
            }
            format.isJavaScript() || format.isCsharp() -> {
                try {
                    val number = s.toDouble()
                    handleAssertionsOnField(number, lines, fieldPath, responseVariableName)
                    return
                } catch (e: NumberFormatException) {
                }

                if (s.equals("true", true) || s.equals("false", true)) {
                    val tf = bodyString.toBoolean()
                    handleAssertionsOnField(tf, lines, fieldPath, responseVariableName)
                    return
                }

                throw IllegalStateException("Cannot parse: $s")
            }
            else -> throw IllegalStateException("Format not supported yet: $format")
        }
    }

    protected fun handleAssertionsOnObject(resContents: Map<String, *>, lines: Lines, fieldPath: String, responseVariableName: String?) {
        if (resContents.isEmpty()) {

            val k = when {
                /*
                    TODO should do check for when there are spaces in the field name
                    TODO also need more tests to check all these edge cases
                 */
                format.isJavaOrKotlin() -> if (fieldPath.isEmpty()) "" else if (fieldPath.startsWith("'")) "$fieldPath." else "'$fieldPath'."
                format.isJavaScript() -> if (fieldPath.isEmpty()) "" else "${if (fieldPath.startsWith("[") || fieldPath.startsWith(".")) "" else "."}$fieldPath"
                format.isCsharp() -> if (fieldPath.isEmpty()) "" else "${if (fieldPath.startsWith("[")) "" else "."}$fieldPath"
                else -> throw IllegalStateException("Format not supported yet: $format")
            }

            val instruction = when {
                //TODO would not this fail on recursive/nested calls???
                format.isJava() -> ".body(\"${k}isEmpty()\", is(true))"
                format.isKotlin() -> ".body(\"${k}isEmpty()\", `is`(true))" //'is' is a keyword in Kotlin
                format.isJavaScript() -> "expect(Object.keys($responseVariableName.body${k}).length).toBe(0);"
                format.isCsharp() -> "Assert.True($responseVariableName${k}.ToString() == \"{}\");"
                else -> throw IllegalStateException("Format not supported yet: $format")
            }

            lines.add(instruction)
        }

        resContents.entries
                .filter { !isFieldToSkip(it.key) }
                .forEach {

                    var needsDot = true

                    val fieldName = if (format.isJava()) {
                        "'${it.key}'"
                    } else if (format.isKotlin()){
                        "'${handleDollarSign(it.key)}'"
                    } else if (format.isJavaScript()) {
                        //field name could have any character... need to use [] notation then
                        if (it.key.matches(Regex("^[a-zA-Z][a-zA-Z0-9]*$"))) {
                            it.key
                        } else {
                            needsDot = false
                            "[\"${it.key}\"]"
                        }
                    //TODO need to deal with '' C#? see EscapeRest
                    } else {
                        it.key
                    }


                    val extendedPath = if (format.isJavaOrKotlin() && fieldPath.isEmpty()) {
                        fieldName
                    } else if (needsDot) {
                        "${fieldPath}.${fieldName}"
                    } else {
                        "${fieldPath}${fieldName}"
                    }

                    handleAssertionsOnField(it.value, lines, extendedPath, responseVariableName)
                }
    }
    /*
        a quick fix on handling dollar sign in assertion
        TODO, might move to other places to systematically handle the assertions with special symbols
     */
    private fun handleDollarSign(text: String): String{
        return text.replace("\$", "\\\$")
    }

    private fun handleAssertionsOnField(value: Any?, lines: Lines, fieldPath: String, responseVariableName: String?) {

        if (value == null) {
            val instruction = when {
                format.isJavaOrKotlin() -> ".body(\"${fieldPath}\", nullValue())"
                format.isJavaScript() -> "expect($responseVariableName.body$fieldPath).toBe(null);"
                format.isCsharp() -> "Assert.True($responseVariableName$fieldPath == null);"
                else -> throw IllegalStateException("Format not supported yet: $format")
            }
            lines.add(instruction)
            return
        }

        when (value) {
            is Map<*, *> -> {
                handleAssertionsOnObject(value as Map<String, *>, lines, fieldPath, responseVariableName)
                return
            }
            is List<*> -> {
                handleAssertionsOnList(value, lines, fieldPath, responseVariableName)
                return
            }
        }

        if (format.isJavaOrKotlin()) {
            val left = when (value) {
                is Boolean -> "equalTo($value)"
                is Number -> "numberMatches($value)"
                is String -> "containsString(" +
                        "\"${GeneUtils.applyEscapes(value as String, mode = GeneUtils.EscapeMode.ASSERTION, format = format)}" +
                        "\")"
                else -> throw IllegalStateException("Unsupported type: ${value::class}")
            }
            if (isSuitableToPrint(left)) {
                lines.add(".body(\"$fieldPath\", $left)")
            }
            return
        }

        if (format.isJavaScript() || format.isCsharp()) {
            val toPrint = if (value is String) {
                "\"" + GeneUtils.applyEscapes(value, mode = GeneUtils.EscapeMode.ASSERTION, format = format) + "\""
            } else {
                value.toString()
            }

            if (isSuitableToPrint(toPrint)) {
                if (format.isJavaScript()) {
                    lines.add("expect($responseVariableName.body$fieldPath).toBe($toPrint);")
                } else {
                    assert(format.isCsharp())
                    if (fieldPath != ".traceId" || !lines.toString().contains("status == 400"))
                        lines.add("Assert.True($responseVariableName$fieldPath == $toPrint);")
                }
            }
            return
        }

        throw IllegalStateException("Not supported format $format")
    }


    protected fun handleAssertionsOnList(list: List<*>, lines: Lines, fieldPath: String, responseVariableName: String?) {

        lines.add(collectionSizeCheck(responseVariableName, fieldPath, list.size))

        //assertions on contents
        if (list.isEmpty()) {
            return
        }

        /*
             TODO could do the same for numbers
         */
        if (format.isJavaOrKotlin() && list.all { it is String } && list.isNotEmpty()) {
            lines.add(".body(\"$fieldPath\", hasItems(${
                (list as List<String>).joinToString {
                    "\"${GeneUtils.applyEscapes(it, mode = GeneUtils.EscapeMode.ASSERTION, format = format)}\""
                }
            }))")
            return
        }

        val limit = if (config.maxAssertionForDataInCollection >= 0) {
            //there are more elements than we can print
            config.maxAssertionForDataInCollection
        } else {
            Int.MAX_VALUE
        }

        val skipped = list.size - limit

        for (i in list.indices) {
            if (i == limit) {
                break
            }
            handleAssertionsOnField(list[i], lines, "$fieldPath[$i]", responseVariableName)
        }
        if (skipped > 0) {
            lines.add("// Skipping assertions on the remaining $skipped elements. This limit of $limit elements can be increased in the configurations")
        }
    }


    protected fun emptyBodyCheck(responseVariableName: String?): String {
        if (format.isJavaOrKotlin()) {
            return ".body(isEmptyOrNullString())"
        }

        if (format.isJavaScript()) {
            /*
                This is super ugly... but there is no clean solution for this
                in Jest nor SuperAgent... :(
                TODO might want to put it in supplementary function
             */
            return "expect(" +
                    "$responseVariableName.body===null " +
                    "|| $responseVariableName.body===undefined " +
                    "|| $responseVariableName.body===\"\" " +
                    "|| Object.keys($responseVariableName.body).length === 0" +
                    ").toBe(true);"
        }

        if (format.isCsharp()) {
            return "Assert.True(string.IsNullOrEmpty(await $responseVariableName.Content.ReadAsStringAsync()));"
        }

        throw IllegalStateException("Unsupported format $format")
    }

    protected fun collectionSizeCheck(responseVariableName: String?, fieldPath: String, expectedSize: Int): String {

        /*
            TODO if size==0, maybe use something like isEmpty?
         */

        val instruction = when {
            format.isJavaOrKotlin() -> {
                val path = if (fieldPath.isEmpty()) "" else "$fieldPath."
                ".body(\"${path}size()\", equalTo($expectedSize))"
            }
            format.isJavaScript() ->
                "expect($responseVariableName.body$fieldPath.length).toBe($expectedSize);"
            format.isCsharp() ->
                "Assert.True($responseVariableName$fieldPath.Count == $expectedSize);"
            else -> throw IllegalStateException("Not supported format $format")
        }

        return instruction
    }

    protected fun bodyIsString(bodyString: String, mode: GeneUtils.EscapeMode, responseVariableName: String?): String {

        val content = GeneUtils.applyEscapes(bodyString, mode, format = format)

        if (format.isJavaOrKotlin()) {
            return ".body(containsString(\"$content\"))"
        }

        if (format.isJavaScript()) {
            return "expect($responseVariableName.text).toBe(\"$content\");"
        }

        if (format.isCsharp()) {
            val k = when {
                content.startsWith("\"") -> content.substring(1, content.length - 1)
                content.startsWith("\\\"") -> content.substring(2, content.length - 2)
                else -> content
            }
            return "Assert.True($responseVariableName == \"$k\");"
        }

        throw IllegalStateException("Not supported format $format")
    }


    /**
     * Some fields might lead to flackiness, eg assertions on timestamps.
     */
    protected fun isFieldToSkip(fieldName: String) =
    //TODO this should be from EMConfig
            /*
                There are some fields like "id" which are often non-deterministic,
                which unfortunately would lead to flaky tests
            */
            listOf(
                    "id",
                    "timestamp", //needed since timestamps will change between runs
                    "self" //TODO: temporary hack. Needed since ports might change between runs.
            ).contains(fieldName.toLowerCase())

    /**
     * Some content may be lead to problems in the resultant test case.
     * Null values, or content that is not yet handled are can lead to un-compilable generated tests.
     * Removing strings that contain "logged" is a stopgap: Some fields mark that particular issues have been logged and will often provide object references and timestamps.
     * Such information can cause failures upon re-run, as object references and timestamps will differ.
     *
     * FIXME need to refactor when dealing with escape refactoring / flaky handling
     */
    protected fun isSuitableToPrint(printableContent: String): Boolean {
        return (
                printableContent != "null" //TODO not so sure about this one... need to double-check
                        && !printableContent.contains("logged")
                        // is this for IP host:port addresses?
                        && !printableContent.contains("""\w+:\d{4,5}""".toRegex()))
    }
}