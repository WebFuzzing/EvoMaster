package org.evomaster.core.output.service

import com.google.gson.Gson
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.CookieWriter
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TokenWriter
import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.problem.httpws.service.HttpWsAction
import org.evomaster.core.problem.httpws.service.HttpWsCallResult
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedAction
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

    protected fun createUniqueBodyVariableName(): String {
        val name = "body_$counter"
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
                && !printableContent.contains("logged")
                // is this for IP host:port addresses?
                && !printableContent.contains("""\w+:\d{4,5}""".toRegex()))
    }


    protected fun handlePreCallSetup(call: HttpWsAction, lines: Lines, res: HttpWsCallResult){
        /*
            This is needed when we need to execute some code before each HTTP call.
            Cannot be in @Before/Fixture, as it must be done for each HTTP call , and not
            just once for test
         */

        if(format.isCsharp()){
            lines.add("Client.DefaultRequestHeaders.Clear();")
            lines.add(getAcceptHeader(call, res))
            lines.addEmpty()
        }
    }

    protected fun handleFirstLine(call: HttpWsAction, lines: Lines, res: HttpWsCallResult, resVarName: String) {

        lines.addEmpty()

        handlePreCallSetup(call, lines, res)

        if (needsResponseVariable(call, res)) {
            when {
                format.isKotlin() -> lines.append("val $resVarName: ValidatableResponse = ")
                format.isJava() -> lines.append("ValidatableResponse $resVarName = ")
                format.isJavaScript() -> lines.append("const $resVarName = ")
                format.isCsharp() -> lines.append("var $resVarName = ")
            }
        }

        when {
            format.isJavaOrKotlin() -> lines.append("given()")
            format.isJavaScript() -> lines.append("await superagent")
            format.isCsharp() -> lines.append("await Client")
        }

        if (!format.isJavaScript() && ! format.isCsharp()) {
            // in JS, the Accept must be after the verb
            // in C#, must be before the call
            lines.append(getAcceptHeader(call, res))
        }
    }


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



    open fun needsResponseVariable(call: HttpWsAction, res: HttpWsCallResult): Boolean {
        /*
          Bit tricky... when using RestAssured on JVM, we can assert directly on the call...
          but that is not the case for the other libraries used for example in JS and C#
         */
        return config.outputFormat == OutputFormat.JS_JEST
                || config.outputFormat == OutputFormat.CSHARP_XUNIT
    }

    protected fun handleHeaders(call: HttpWsAction, lines: Lines) {

        if(format.isCsharp()){
            //FIXME
            log.warn("Currently not handling headers in C#")
            return
        }

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



    protected fun handleResponseAfterTheCall(call: HttpWsAction, res: HttpWsCallResult, responseVariableName: String, lines: Lines) {

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
            format.isCsharp() ->{
                lines.add("Assert.Equal($code, (int) $responseVariableName.StatusCode);")
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "No status assertion supported for format $format")
            }
        }

        handleLastStatementComment(res, lines)

        if (config.enableBasicAssertions) {
            handleResponseAssertions(lines, res, responseVariableName)
        }
    }

    protected open fun handleLastStatementComment(res: HttpWsCallResult, lines: Lines){
        val code = res.getStatusCode()
        if (code == 500 && ! config.blackBox) {
            lines.append(" // " + res.getLastStatementWhen500())
        }
    }

    protected fun handleSingleCall(
            evaluatedAction: EvaluatedAction,
            lines: Lines,
            baseUrlOfSut: String
    ) {
        lines.addEmpty()

        val call = evaluatedAction.action as HttpWsAction
        val res = evaluatedAction.result as HttpWsCallResult

        if (res.failedCall()) {
            addActionInTryCatch(call, lines, res, baseUrlOfSut)
        } else {
            addActionLines(call, lines, res, baseUrlOfSut)
        }
    }

    protected fun makeHttpCall(call: HttpWsAction, lines: Lines, res: HttpWsCallResult, baseUrlOfSut: String): String {
        //first handle the first line
        val responseVariableName = createUniqueResponseVariableName()

        handleFirstLine(call, lines, res, responseVariableName)

        when {
            format.isJavaOrKotlin() -> {
                lines.indent(2)
                handleHeaders(call, lines)
                handleBody(call, lines)
                handleVerbEndpoint(baseUrlOfSut, call, lines)
            }
            format.isJavaScript() -> {
                lines.indent(2)
                //in SuperAgent, verb must be first
                handleVerbEndpoint(baseUrlOfSut, call, lines)
                lines.append(getAcceptHeader(call, res))
                handleHeaders(call, lines)
                handleBody(call, lines)
            }
            format.isCsharp() -> {
                handleVerbEndpoint(baseUrlOfSut, call, lines)
                //TODO headers
            }
        }

        if (format.isJavaOrKotlin()) {
            handleResponseDirectlyInTheCall(call, res, lines)
        }
        handleLastLine(call, res, lines, responseVariableName)
        return responseVariableName
    }



    abstract fun handleVerbEndpoint(baseUrlOfSut: String, _call: HttpWsAction, lines: Lines)

    protected fun sendBodyCommand() : String{
        return when {
            format.isJavaOrKotlin() -> "body"
            format.isJavaScript() -> "send"
            format.isCsharp() -> ""
            else -> throw IllegalArgumentException("Format not supported $format")
        }
    }

    //TODO: check again for C#, especially when not json
    protected open fun handleBody(call: HttpWsAction, lines: Lines) {

        val bodyParam = call.parameters.find { p -> p is BodyParam } as BodyParam?


        if(format.isCsharp() && bodyParam == null){
            lines.append("null")
            return
        }

        if (bodyParam != null) {

            val send = sendBodyCommand()

            when {
                format.isJavaOrKotlin() -> lines.add(".contentType(\"${bodyParam.contentType()}\")")
                format.isJavaScript() -> lines.add(".set('Content-Type','${bodyParam.contentType()}')")
                //FIXME
            //format.isCsharp() -> lines.add("Client.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue(\"${bodyParam.contentType()}\"));")
            }

            if (bodyParam.isJson()) {

                val json = bodyParam.gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = format)

                 printSendJsonBody(json, lines)

            } else if (bodyParam.isTextPlain()) {
                val body =
                        bodyParam.gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.TEXT, targetFormat = format)
                if (body != "\"\"") {
                    if (!format.isCsharp())
                        lines.add(".$send($body)")
                    else {
                        lines.append("new StringContent(\"$body\", Encoding.UTF8, \"${bodyParam.contentType()}\")")
                    }
                } else {
                    if (!format.isCsharp())
                        lines.add(".$send(\"${"""\"\""""}\")")
                    else {
                        lines.append("new StringContent(\"${"""\"\""""}\", Encoding.UTF8, \"${bodyParam.contentType()}\")")
                    }
                }

                //BMR: this is needed because, if the string is empty, it causes a 400 (bad request) code on the test end.
                // inserting \"\" should prevent that problem
                // TODO: get some tests done of this

            } else if (bodyParam.isForm()) {
                val body = bodyParam.gene.getValueAsPrintableString(
                        mode = GeneUtils.EscapeMode.X_WWW_FORM_URLENCODED,
                        targetFormat = format
                )
                if (!format.isCsharp())
                    lines.add(".$send(\"$body\")")
                else {
                    lines.append("new StringContent(\"$body\", Encoding.UTF8, \"${bodyParam.contentType()}\")")
                }

            } else {
                //TODO XML
                LoggingUtil.uniqueWarn(log, "Unhandled type for body payload: " + bodyParam.contentType())
            }
        }
    }

    fun printSendJsonBody(json: String, lines: Lines) {

        val send = sendBodyCommand()

        val body = if (OutputFormatter.JSON_FORMATTER.isValid(json)) {
            OutputFormatter.JSON_FORMATTER.getFormatted(json)
        } else {
            json
        }

        //needed as JSON uses ""
        val bodyLines = body.split("\n").map { s ->
            "\" " + GeneUtils.applyEscapes(s.trim(), mode = GeneUtils.EscapeMode.BODY, format = format) + " \""
        }

        if (bodyLines.size == 1) {
            if (!format.isCsharp()) {
                lines.add(".$send(${bodyLines.first()})")
            } else {
                lines.add("new StringContent(${bodyLines.first()}, Encoding.UTF8, \"application/json\")")
            }
        } else {
            if (!format.isCsharp()) {
                lines.add(".$send(${bodyLines.first()} + ")
                lines.indented {
                    (1 until bodyLines.lastIndex).forEach { i ->
                        lines.add("${bodyLines[i]} + ")
                    }
                    lines.add("${bodyLines.last()})")
                }
            } else {
                lines.add("new StringContent(")
                lines.add("${bodyLines.first()} +")
                lines.indented {
                    (1 until bodyLines.lastIndex).forEach { i ->
                        lines.add("${bodyLines[i]} + ")
                    }
                    lines.add("${bodyLines.last()}")
                }
                lines.add(", Encoding.UTF8, \"application/json\")")
            }
        }
    }

    /**
     * This is done mainly for RestAssured
     */
    protected fun handleResponseDirectlyInTheCall(call: HttpWsAction, res: HttpWsCallResult, lines: Lines) {
        if (!res.failedCall()) {

            val code = res.getStatusCode()

            when {
                format.isJavaOrKotlin() -> {
                    lines.add(".then()")
                    lines.add(".statusCode($code)")
                }
                else -> throw IllegalStateException("No assertion in calls for format: $format")
            }

            handleLastStatementComment(res, lines)

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
                format.isCsharp() -> "Assert.Contains(\"$bodyTypeSimplified\", $responseVariableName.Content.Headers.GetValues(\"Content-Type\").First());"
                else -> throw IllegalStateException("Unsupported format $format")
            }
            lines.add(instruction)
        }

        val bodyString = res.getBody()

        if (res.getBodyType() != null) {
            val type = res.getBodyType()!!

            var bodyVarName = responseVariableName

            if(format.isCsharp()){
                //cannot use response object directly, as need to unmarshall the body payload manually
                bodyVarName = createUniqueBodyVariableName()
                lines.add("dynamic $bodyVarName = ")
            }

            if (type.isCompatible(MediaType.APPLICATION_JSON_TYPE) || type.toString().toLowerCase().contains("+json")) {

                if(format.isCsharp()){
                    lines.append("JsonConvert.DeserializeObject(await $responseVariableName.Content.ReadAsStringAsync());")
                }

                when (bodyString?.trim()?.first()) {
                    //TODO this should be handled recursively, and not ad-hoc here...
                    '[' -> {
                        // This would be run if the JSON contains an array of objects.
                        val list = Gson().fromJson(res.getBody(), List::class.java)
                        handleAssertionsOnList(list, lines, "", bodyVarName)
                    }
                    '{' -> {
                        // JSON contains an object
                        val resContents = Gson().fromJson(res.getBody(), Map::class.java)
                        handleAssertionsOnObject(resContents as Map<String, *>, lines, "", bodyVarName)
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
                            res.getTooLargeBody() -> lines.add("/* very large body, which was not handled during the search */")

                            bodyString.isNullOrBlank() -> lines.add(emptyBodyCheck(bodyVarName))

                            else -> handlePrimitive(lines, bodyString, "", bodyVarName)
                        }
                    }
                }
            } else if (type.isCompatible(MediaType.TEXT_PLAIN_TYPE)) {

                if(format.isCsharp()){
                    lines.append("await $responseVariableName.Content.ReadAsStringAsync();")
                }

                if (bodyString.isNullOrBlank()) {
                    lines.add(emptyBodyCheck(bodyVarName))
                } else {
                    //TODO in the call above BODY was used... what's difference from TEXT?
                    lines.add(bodyIsString(bodyString, GeneUtils.EscapeMode.TEXT, bodyVarName))
                }
            } else {
                if(format.isCsharp()){
                    lines.append("await $responseVariableName.Content.ReadAsStringAsync();")
                }
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
            format.isJavaScript() || format.isCsharp() -> {
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
            else -> throw IllegalStateException("Format not supported yet: $format")
        }
    }

    protected fun handleAssertionsOnObject(resContents: Map<String, *>, lines: Lines, fieldPath: String, responseVariableName: String?) {
        if (resContents.isEmpty()) {

            val k = when{
                /*
                    TODO should do check for when there are spaces in the field name
                    TODO also need more tests to check all these edge cases
                 */
                format.isJavaOrKotlin() -> if(fieldPath.isEmpty()) "" else if(fieldPath.startsWith("'")) "$fieldPath." else "'$fieldPath'."
                format.isJavaScript() -> if(fieldPath.isEmpty()) "" else "${if(fieldPath.startsWith("[")|| fieldPath.startsWith("."))"" else "."}$fieldPath"
                format.isCsharp()  -> if(fieldPath.isEmpty()) "" else "${if(fieldPath.startsWith("["))"" else "."}$fieldPath"
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

        if(format.isJavaScript() || format.isCsharp()){
            val toPrint = if(value is String){
                "\""+GeneUtils.applyEscapes(value, mode = GeneUtils.EscapeMode.ASSERTION, format = format)+"\""
            } else {
                value.toString()
            }

            if(isSuitableToPrint(toPrint)) {
                if(format.isJavaScript()) {
                    lines.add("expect($responseVariableName.body$fieldPath).toBe($toPrint);")
                } else {
                    assert(format.isCsharp())
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

        list.forEachIndexed {  index, value ->
            handleAssertionsOnField(value, lines, "$fieldPath[$index]", responseVariableName)
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

        if(format.isCsharp()){
            return "Assert.True(await $responseVariableName.Content.ReadAsStringAsync() == \"\" );"
        }

       throw IllegalStateException("Unsupported format $format")
    }

    protected fun collectionSizeCheck(responseVariableName: String?, fieldPath: String, expectedSize: Int): String {

        /*
            TODO if size==0, maybe use something like isEmpty?
         */

        val instruction = when {
            format.isJavaOrKotlin() -> {
                val path = if(fieldPath.isEmpty()) "" else "$fieldPath."
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

        if(format.isCsharp()){
            val k = when {
                content.startsWith("\"") -> content.substring(1, content.length-1)
                content.startsWith("\\\"") -> content.substring(2, content.length-2)
                else -> content
            }
            return "Assert.True($responseVariableName == \"$k\");"
        }

        throw IllegalStateException("Not supported format $format")
    }

    protected fun handleLastLine(call: HttpWsAction, res: HttpWsCallResult, lines: Lines, resVarName: String) {

        if(format.isJavaScript()){
            /*
                This is to deal with very weird behavior in SuperAgent that crashes the tests
                for status codes different from 2xx...
                so, here we make it passes as long as a status was present
             */
            lines.add(".ok(res => res.status)")
        }

        if (config.enableBasicAssertions) {
            lines.appendSemicolon(format)
        }

        if(!format.isCsharp()) {
            lines.deindent(2)
        }
    }
}