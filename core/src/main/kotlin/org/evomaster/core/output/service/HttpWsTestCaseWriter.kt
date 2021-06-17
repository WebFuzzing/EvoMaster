package org.evomaster.core.output.service

import com.google.gson.Gson
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.CookieWriter
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TokenWriter
import org.evomaster.core.problem.httpws.service.HttpWsAction
import org.evomaster.core.problem.httpws.service.HttpWsCallResult
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.gene.GeneUtils
import org.slf4j.LoggerFactory
import java.lang.NumberFormatException
import javax.ws.rs.core.MediaType

abstract class HttpWsTestCaseWriter : WebTestCaseWriter() {

    companion object {
        private val log = LoggerFactory.getLogger(HttpWsTestCaseWriter::class.java)
    }

    abstract fun getAcceptHeader(call: HttpWsAction, res: HttpWsCallResult): String


    override fun shouldFailIfException(result: ActionResult): Boolean {
        /*
            Fail test if exception is not thrown, but not if it was a timeout,
            otherwise the test would become flaky
        */
        return !(result as HttpWsCallResult).getTimedout()
    }

    protected fun createUniqueResponseVariableName(): String {
        val name = "res_$counter"
        counter++
        return name
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
     */
    protected fun isSuitableToPrint(printableContent: String): Boolean {
        return (
                printableContent != "null" //TODO not so sure about this one... need to double-check
                && printableContent != NOT_COVERED_YET
                && !printableContent.contains("logged")
                // is this for IP host:port addresses?
                && !printableContent.contains("""\w+:\d{4,5}""".toRegex()))
    }


    protected fun hasFieldToSkip(fieldNames: Collection<*>) = fieldNames.any { it is String && isFieldToSkip(it) }

    protected fun isVerbWithPossibleBodyPayload(verb: String): Boolean {

        val verbs = arrayOf("post", "put", "patch")

        if (verbs.contains(verb.toLowerCase()))
            return true;
        return false;
    }

    protected fun openAcceptHeader(): String {
        return when {
            format.isJavaOrKotlin() -> ".accept("
            format.isJavaScript() -> ".set('Accept', "
            format.isCsharp() -> "Client.DefaultRequestHeaders.Add(\"Accept\", "
            else -> throw IllegalArgumentException("Invalid format: $format")
        }
    }

    protected fun closeAcceptHeader(openedHeader: String): String {
        var result = openedHeader
        result += ")"
        if (format.isCsharp()) result = "$result;"
        return result
    }

    protected fun handleFirstLine(call: RestCallAction, lines: Lines, res: RestCallResult, resVarName: String) {

        lines.addEmpty()
        if (needsResponseVariable(call, res)) {
            when {
                format.isKotlin() -> lines.append("val $resVarName: ValidatableResponse = ")
                format.isJava() -> lines.append("ValidatableResponse $resVarName = ")
                format.isJavaScript() -> lines.append("const $resVarName = ")
                //TODO C#
            }
        }

        when {
            format.isJavaOrKotlin() -> lines.append("given()")
            format.isJavaScript() -> lines.append("await superagent")
            format.isCsharp() -> lines.append("Client.DefaultRequestHeaders.Clear();\n")
        }

        if (!format.isJavaScript()) {
            // in JS, the Accept must be after the verb
            lines.append(getAcceptHeader(call, res))
        }
    }


    open fun needsResponseVariable(call: HttpWsAction, res: HttpWsCallResult): Boolean {
        /*
          Bit tricky... when using RestAssured on JVM, we can assert directly on the call...
          but that is not the case for the other libraries used for example in JS and C#
         */
        return config.outputFormat == OutputFormat.JS_JEST
                || config.outputFormat == OutputFormat.CSHARP_XUNIT
    }

    protected fun handleHeaders(call: HttpWsAction, lines: Lines) {

        val prechosenAuthHeaders = call.auth.headers.map { it.name }

        val set = when {
            format.isJavaOrKotlin() -> "header"
            format.isJavaScript() -> "set"
            //TODO C#
            else -> throw IllegalArgumentException("Not supported format: $format")
        }

        call.auth.headers.forEach {
            lines.add(".$set(\"${it.name}\", \"${it.value}\") // ${call.auth.name}")
        }

        call.parameters.filterIsInstance<HeaderParam>()
                .filter { !prechosenAuthHeaders.contains(it.name) }
                .filter { !(call.auth.jsonTokenPostLogin != null && it.name.equals("Authorization", true)) }
                .forEach {
                    lines.add(".$set(\"${it.name}\", ${it.gene.getValueAsPrintableString(targetFormat = format)})")
                }

        val cookieLogin = call.auth.cookieLogin
        if (cookieLogin != null) {
            when {
                format.isJavaOrKotlin() -> lines.add(".cookies(${CookieWriter.cookiesName(cookieLogin)})")
                format.isJavaScript() -> lines.add(".set('Cookies', ${CookieWriter.cookiesName(cookieLogin)})")
                //TODO C#
            }
        }

        //TODO make sure header was not already set
        val tokenLogin = call.auth.jsonTokenPostLogin
        if (tokenLogin != null) {
            lines.add(".$set(\"Authorization\", ${TokenWriter.tokenName(tokenLogin)}) // ${call.auth.name}")
        }
    }

    /**
     * This is done mainly for RestAssured
     */
    protected fun handleResponseDirectlyInTheCall(call: HttpWsAction, res: RestCallResult, lines: Lines) {
        if (!res.failedCall()) {

            val code = res.getStatusCode()

            when {
                format.isJavaOrKotlin() -> {
                    lines.add(".then()")
                    lines.add(".statusCode($code)")
                }
                else -> throw IllegalStateException("No assertion in calls for format: $format")
                //format.isCsharp() -> lines.add("Assert.Equal($code, (int) response.StatusCode);")
            }

            if (code == 500) {
                lines.append(" // " + res.getLastStatementWhen500())
            }

            if (config.enableBasicAssertions) {
                handleResponseAssertions(lines, res, null)
            }

        }

//        else if (partialOracles.generatesExpectation(call, res)
//                && format.isJavaOrKotlin()){
//            //FIXME what is this for???
//            lines.add(".then()")
//        }
    }

    protected fun handleResponseAfterTheCall(call: RestCallAction, res: RestCallResult, responseVariableName: String, lines: Lines) {

        if (format.isJavaOrKotlin() //assertions handled in the call
                || !needsResponseVariable(call, res)
                || res.failedCall()
        ) {
            return
        }

        lines.addEmpty()

        val code = res.getStatusCode()

        when {
            format.isJavaScript() -> {
                lines.add("expect($responseVariableName.status).toBe($code);")
            }
            //TODO C#
            else -> {
                LoggingUtil.uniqueWarn(log, "No status assertion supported for format $format")
            }
        }

        if (code == 500) {
            lines.append(" // " + res.getLastStatementWhen500())
        }

        if (config.enableBasicAssertions) {
            handleResponseAssertions(lines, res, responseVariableName)
        }
    }


    //----------------------------------------------------------------------------------------
    // assertion lines

    protected fun handleResponseAssertions(lines: Lines, res: HttpWsCallResult, responseVariableName: String?) {

        assert(responseVariableName != null || format.isJavaOrKotlin())

        /*
            there are 2 cases:
            a) assertions directly as part of the HTTP call, eg, as done in RestAssured
            b) assertions on response object, stored in a variable after the HTTP call

            based on this, the code to add is quite different.
            Note, in case of (b), we must have the name of the variable
         */
        val isInCall = responseVariableName == null

        if (isInCall) {
            lines.add(".assertThat()")
        }

        if (res.getBodyType() == null) {
            lines.add(emptyBodyCheck(responseVariableName))
        } else {

            //TODO is there a better solution? where was this a problem?
            val bodyTypeSimplified = res.getBodyType()
                    .toString()
                    .split(";") // remove all associated variables
                    .first()

            val instruction = when {
                format.isJavaOrKotlin() -> ".contentType(\"$bodyTypeSimplified\")"
                format.isJavaScript() ->
                    "expect($responseVariableName.header[\"content-type\"].startsWith(\"$bodyTypeSimplified\")).toBe(true);"
                else -> throw IllegalStateException("Unsupported format $format")
            }
            lines.add(instruction)
        }

        val bodyString = res.getBody()

        if (res.getBodyType() != null) {
            val type = res.getBodyType()!!
            if (type.isCompatible(MediaType.APPLICATION_JSON_TYPE) || type.toString().toLowerCase().contains("+json")) {
                when (bodyString?.trim()?.first()) {
                    //TODO this should be handled recursively, and not ad-hoc here...
                    '[' -> {
                        // This would be run if the JSON contains an array of objects.
                        val list = Gson().fromJson(res.getBody(), List::class.java)
                        handleAssertionsOnList(list, lines, "", responseVariableName)
                    }
                    '{' -> {
                        // JSON contains an object
                        val resContents = Gson().fromJson(res.getBody(), Map::class.java)
                        handleAssertionsOnObject(resContents as Map<String, *>, lines, "", responseVariableName)
                    }
                    '"' -> {
                        lines.add(bodyIsString(bodyString, GeneUtils.EscapeMode.BODY, responseVariableName))
                    }
                    else -> {
                        /*
                            This branch will be called if the JSON is null (or has a basic type)
                            Currently, it converts the contents to String.

                            TODO do we have tests for it? and if it is true for RestAssured, anyway
                            it does not the seem the case for Jest/SuperAgent
                         */
                        when {
                            res.getTooLargeBody() -> lines.add("/* very large body, which was not handled during the search */")

                            bodyString.isNullOrBlank() -> lines.add(emptyBodyCheck(responseVariableName))

                            else -> handlePrimitive(lines, bodyString, "", responseVariableName)
                        }
                    }
                }
            } else if (type.isCompatible(MediaType.TEXT_PLAIN_TYPE)) {
                if (bodyString.isNullOrBlank()) {
                    lines.add(emptyBodyCheck(responseVariableName))
                } else {
                    //TODO in the call above BODY was used... what's difference from TEXT?
                    lines.add(bodyIsString(bodyString, GeneUtils.EscapeMode.TEXT, responseVariableName))
                }
            } else {
                LoggingUtil.uniqueWarn(log, "Currently no assertions are generated for response type: $type")
            }
        }
    }

    private fun handlePrimitive(lines: Lines, bodyString: String, fieldPath: String, responseVariableName: String?) {

        val s = bodyString.trim()

        when{
            format.isJavaOrKotlin() -> {
                lines.add(bodyIsString(s, GeneUtils.EscapeMode.BODY, responseVariableName))
            }
            format.isJavaScript() -> {
                try {
                    val number = s.toDouble()
                    handleAssertionsOnField(number, lines, fieldPath, responseVariableName)
                    return
                } catch (e: NumberFormatException){
                }

                if(s.equals("true", true) || s.equals("false", true)) {
                    val tf = bodyString.toBoolean()
                    handleAssertionsOnField(tf, lines, fieldPath, responseVariableName)
                    return
                }

                throw IllegalStateException("Cannot parse: $s")
            }
            //TODO C#
            else -> throw IllegalStateException("Format not supported yet: $format")
        }
    }

    protected fun handleAssertionsOnObject(resContents: Map<String, *>, lines: Lines, fieldPath: String, responseVariableName: String?) {
        if (resContents.isEmpty()) {

            val k = when{
                format.isJavaOrKotlin() -> if(fieldPath.isEmpty()) "" else "'$fieldPath'."
                format.isJavaScript() -> if(fieldPath.isEmpty()) "" else ".$fieldPath"
                //TODO C#
                else -> throw IllegalStateException("Format not supported yet: $format")
            }

            val instruction = when {
                //TODO would not this fail on recursive/nested calls???
                format.isJava() -> ".body(\"${k}isEmpty()\", is(true))"
                format.isKotlin() -> ".body(\"${k}isEmpty()\", `is`(true))" //'is' is a keyword in Kotlin
                format.isJavaScript() -> "expect(Object.keys($responseVariableName.body${k}).length).toBe(0);"
                //TODO C#
                else -> throw IllegalStateException("Format not supported yet: $format")
            }

            lines.add(instruction)
        }

        resContents.entries
                .filter { !isFieldToSkip(it.key) }
                .forEach {
                    //TODO should in JavaKotlin have the new fields inside ''? what was old reason for that?
                    val fieldName = if(format.isJavaOrKotlin()){
                        "'${it.key}'"
                        //TODO need to deal with '' as well in JS/C#? see EscapeRest
                    } else {
                        it.key
                    }

                    val extendedPath = if(format.isJavaOrKotlin() && fieldPath.isEmpty()){
                        fieldName
                    } else {
                        "$fieldPath.${fieldName}"
                    }
                    handleAssertionsOnField(it.value, lines, extendedPath, responseVariableName)
                }
    }

    private fun handleAssertionsOnField(value: Any?, lines: Lines, fieldPath: String, responseVariableName: String?) {

        if (value == null) {
            val instruction = when {
                format.isJavaOrKotlin() -> ".body(\"${fieldPath}\", nullValue())"
                format.isJavaScript() -> "expect($responseVariableName$fieldPath).toBe(null);"
                //TODO C#
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

        if(format.isJavaOrKotlin()) {
            val  left = when (value) {
                is Boolean -> "equalTo($value)"
                is Number -> "numberMatches($value)"
                is String -> "containsString(" +
                        "\"${GeneUtils.applyEscapes(value as String, mode = GeneUtils.EscapeMode.ASSERTION, format = format)}" +
                        "\")"
                else -> throw IllegalStateException("Unsupported type: ${value::class}")
            }
            if(isSuitableToPrint(left)){
                lines.add(".body(\"$fieldPath\", $left)")
            }
            return
        }

        if(format.isJavaScript()){
            val toPrint = if(value is String){
                "\""+GeneUtils.applyEscapes(value, mode = GeneUtils.EscapeMode.ASSERTION, format = format)+"\""
            } else {
                value.toString()
            }
            if(isSuitableToPrint(toPrint)) {
                lines.add("expect($responseVariableName.body$fieldPath).toBe($toPrint);")
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
        if (format.isJavaOrKotlin() && (list as List<*>).all { it is String } && list.isNotEmpty()) {
            lines.add(".body(\"$fieldPath\", hasItems(${
                (list as List<String>).joinToString {
                    "\"${GeneUtils.applyEscapes(it, mode = GeneUtils.EscapeMode.ASSERTION, format = format)}\""
                }
            }))")
            return
        }

        list.forEachIndexed {  index, value ->
            handleAssertionsOnField(value, lines, "$fieldPath[$index]", responseVariableName)
        }
    }


    protected fun handleFieldValues_getMatcher(resContentsItem: Any?): String {
        /* BMR: this code is due to a somewhat unfortunate problem:
        - Gson parses all numbers as Double (NOTE: this is expected, as JSON/JS has only Double for numbers)
        - Hamcrest has a hard time comparing double to int
        The solution (for JVM) is to use an additional content matcher that can be found in NumberMatcher.
        This can also be used as a template for adding more matchers, should such a step be needed.
        * */
        if (resContentsItem == null) {
            return "nullValue()"
        } else {
            when (resContentsItem::class) {
                Double::class -> return "numberMatches(${resContentsItem as Double})"
                String::class -> return "containsString(" +
                        "\"${GeneUtils.applyEscapes(resContentsItem as String, mode = GeneUtils.EscapeMode.ASSERTION, format = format)}" +
                        "\")"
                Map::class -> return NOT_COVERED_YET
                ArrayList::class -> {
                    if ((resContentsItem as ArrayList<*>).all { it is String } && resContentsItem.isNotEmpty()) {
                        return "hasItems(${
                            (resContentsItem as ArrayList<String>).joinToString {
                                "\"${GeneUtils.applyEscapes(it, mode = GeneUtils.EscapeMode.ASSERTION, format = format)}\""
                            }
                        })"
                    } else {
                        return NOT_COVERED_YET
                    }
                }
                else -> return NOT_COVERED_YET
            }
        }
    }


    protected fun handleMapLines(index: Int, map: Map<*, *>, lines: Lines) {
        map.keys.forEach {
            val printableTh = handleFieldValues_getMatcher(map[it])
            if (isSuitableToPrint(printableTh)) {
                lines.add(".body(\"\'$it\'\", hasItem($printableTh))")
            }
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

        //TODO C#
        return "TODO"
    }

    protected fun collectionSizeCheck(responseVariableName: String?, fieldPath: String, expectedSize: Int): String {

        /*
            TODO if size==0, maybe use something like isEmpty?
         */

        val instruction = when {
            format.isJavaOrKotlin() -> ".body($fieldPath\"size()\", equalTo($expectedSize))"
            format.isJavaScript() ->
                "expect($responseVariableName.body$fieldPath.length).toBe($expectedSize);"
            //TODO C#
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

        //TODO C#

        LoggingUtil.uniqueWarn(log, "Not supported format $format")
        return "TODO"
    }



}