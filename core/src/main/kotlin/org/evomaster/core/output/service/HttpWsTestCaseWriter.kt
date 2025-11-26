package org.evomaster.core.output.service

import com.google.inject.Inject
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.JsonUtils
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.output.TestWriterUtils.formatJsonWithEscapes
import org.evomaster.core.output.auth.CookieWriter
import org.evomaster.core.output.auth.TokenWriter
import org.evomaster.core.output.dto.DtoCall
import org.evomaster.core.output.dto.GeneToDto
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.security.data.ActionStubMapping
import org.evomaster.core.problem.security.service.HttpCallbackVerifier
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.action.EvaluatedAction
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.slf4j.LoggerFactory
import java.nio.file.Path
import javax.ws.rs.core.MediaType
import kotlin.collections.filter


abstract class HttpWsTestCaseWriter : ApiTestCaseWriter() {

    @Inject
    private lateinit var httpCallbackVerifier: HttpCallbackVerifier

    companion object {
        private val log = LoggerFactory.getLogger(HttpWsTestCaseWriter::class.java)
    }

    abstract fun getAcceptHeader(call: HttpWsAction, res: HttpWsCallResult): String


    override fun shouldFailIfExceptionNotThrown(result: ActionResult): Boolean {
        /*
            Fail test if exception is not thrown, but not if it was a timeout,
            otherwise the test would become flaky
        */
        return !(result as HttpWsCallResult).getTimedout()
    }

    fun startRequest(lines: Lines){
        when {
            format.isJavaOrKotlin() -> lines.append("given()")
            format.isJavaScript() -> lines.append("await superagent")
            format.isCsharp() -> lines.append("await Client")
            format.isPython() -> lines.append("requests \\")
        }
    }

    override fun handleTestInitialization(
        lines: Lines,
        baseUrlOfSut: String,
        ind: EvaluatedIndividual<*>,
        insertionVars: MutableList<Pair<String, String>>,
        testName: String
    ) {
        CookieWriter.handleGettingCookies(format, ind, lines, baseUrlOfSut, this)
        TokenWriter.handleGettingTokens(format, ind, lines, baseUrlOfSut, this)

        super.handleTestInitialization(lines, baseUrlOfSut, ind, insertionVars,testName)
    }

    protected fun handlePreCallSetup(call: HttpWsAction, lines: Lines, res: HttpWsCallResult) {
        /*
            This is needed when we need to execute some code before each HTTP call.
            Cannot be in @Before/Fixture, as it must be done for each HTTP call , and not
            just once for test
         */

        if (format.isCsharp()) {
            lines.add("Client.DefaultRequestHeaders.Clear();")
            lines.add(getAcceptHeader(call, res))
            lines.addEmpty()
        }
    }

    protected fun handleFirstLine(call: HttpWsAction, lines: Lines, res: HttpWsCallResult, resVarName: String) {

        lines.addEmpty()

        handlePreCallSetup(call, lines, res)

        if (needsResponseVariable(call, res) && !res.failedCall()) {
            when {
                format.isKotlin() -> lines.append("val $resVarName: ValidatableResponse = ")
                format.isJava() -> lines.append("ValidatableResponse $resVarName = ")
                format.isJavaScript() -> lines.append("const $resVarName = ")
                format.isPython() -> lines.append("$resVarName = ")
                format.isCsharp() -> lines.append("var $resVarName = ")
            }
        }

        when {
            format.isJavaOrKotlin() -> lines.append("given()")
            format.isJavaScript() -> lines.append("await superagent")
            format.isCsharp() -> lines.append("await Client")
            format.isPython() -> lines.append("requests \\")
        }

        if (!format.isJavaScript() && !format.isCsharp() && !format.isPython()) {
            // in JS, the Accept must be after the verb
            // in C# and Python, must be before the call
            lines.append(getAcceptHeader(call, res))
        }
    }

    private fun writeDto(call: HttpWsAction, lines: Lines): String {
        val bodyParam = call.parameters.find { p -> p is BodyParam } as BodyParam?
        if (bodyParam != null && bodyParam.isJson()) {

            val primaryGene = bodyParam.primaryGene()
            val choiceGene = primaryGene.getWrappedGene(ChoiceGene::class.java)
            val actionName = call.getName()
            if (choiceGene != null) {
                // We only generate DTOs for ChoiceGene objects that contain either an ObjectGene or ArrayGene in their
                // genes. This check is necessary since when using `example` and `default` entries,
                // "primitive" genes are represented as ChoiceGene with  an EnumGene and the actual
                // String/Integer/Number/etc gene
                if (hasObjectOrArrayGene(choiceGene)) {
                    return generateDtoCall(choiceGene, actionName, lines).varName
                }
            } else {
                val leafGene = primaryGene.getLeafGene()
                if (leafGene is ObjectGene || leafGene is ArrayGene<*>) {
                    return generateDtoCall(leafGene, actionName, lines).varName
                }
            }

        }
        return ""
    }

    private fun generateDtoCall(gene: Gene, actionName: String, lines: Lines): DtoCall {
        val geneToDto = GeneToDto(format)

        val dtoName = geneToDto.getDtoName(gene, actionName, false)
        val dtoCall = geneToDto.getDtoCall(gene, dtoName, mutableListOf(counter++), false)

        dtoCall.objectCalls.forEach {
            lines.add(it)
        }
        lines.addEmpty()
        return dtoCall
    }

    private fun hasObjectOrArrayGene(gene: ChoiceGene<*>): Boolean {
        return gene.getViewOfChildren().any { it is ObjectGene || it is ArrayGene<*> }
    }

    protected fun isVerbWithPossibleBodyPayload(verb: String): Boolean {

        val verbs = arrayOf("post", "put", "patch")

        if (verbs.contains(verb.lowercase()))
            return true;
        return false;
    }

    protected fun openAcceptHeader(): String {
        return when {
            format.isJavaOrKotlin() -> ".accept("
            format.isJavaScript() -> ".set('Accept', "
            format.isCsharp() -> "Client.DefaultRequestHeaders.Add(\"Accept\", "
            format.isPython() -> "headers['Accept'] = "
            else -> throw IllegalArgumentException("Invalid format: $format")
        }
    }

    protected fun closeAcceptHeader(openedHeader: String): String {
        var result = openedHeader
        if (!config.outputFormat.isPython()) {
            result += ")"
        }
        if (format.isCsharp()){
            result = "$result;"
        }
        return result
    }


    open fun needsResponseVariable(call: HttpWsAction, res: HttpWsCallResult): Boolean {
        /*
          Bit tricky... when using RestAssured on JVM, we can assert directly on the call...
          but that is not the case for the other libraries used for example in JS and C#
         */
        return config.enableBasicAssertions &&
                (config.outputFormat == OutputFormat.JS_JEST || config.outputFormat == OutputFormat.PYTHON_UNITTEST)
    }

    protected fun handleHeaders(call: HttpWsAction, lines: Lines) {

        //TODO handle REST links

        if (format.isCsharp()) {
            log.warn("Currently not handling headers in C#")
            return
        }

        val prechosenAuthHeaders = call.auth.headers.map { it.name }

        val set = when {
            format.isJavaOrKotlin() -> "header"
            format.isJavaScript() -> "set"
            format.isPython() -> "headers = {}"
            else -> throw IllegalArgumentException("Not supported format: $format")
        }

        if (format.isPython()) {
            lines.add(set)
        }

        //headers in specified auth info
        call.auth.headers.forEach {
            if (format.isPython()) {
                lines.add("headers[\"${it.name}\"] = \"${it.value}\"")
            } else {
                lines.add(".$set(\"${it.name}\", \"${it.value}\") // ${call.auth.name}")
            }
        }

        val elc = call.auth.endpointCallLogin

        //headers from schema
        call.parameters.filterIsInstance<HeaderParam>()
            .filter { !prechosenAuthHeaders.contains(it.name) }
            .filter { elc?.token == null || !(it.name.equals(elc.token.httpHeaderName, true)) }
            .filter { it.isInUse() }
            .forEach {
                val x = it.getRawValue()
                val escapedHeader = GeneUtils.applyEscapes(x, GeneUtils.EscapeMode.BODY, format)
                if (format.isPython()) {
                    lines.add("headers[\"${it.name}\"] = \"${escapedHeader}\"")
                } else {

                    lines.add(".$set(\"${it.name}\", \"${escapedHeader}\")")
                }
            }

        if (elc != null) {

            if (!elc.expectsCookie()) {
                val tokenHeader = elc.token!!.httpHeaderName
                if (format.isPython()) {
                    lines.add("headers[\"$tokenHeader\"] = ${TokenWriter.tokenName(elc)} # ${call.auth.name}")
                } else {
                    lines.add(".$set(\"$tokenHeader\", ${TokenWriter.tokenName(elc)}) // ${call.auth.name}")
                }
            } else {
                when {
                    format.isJavaOrKotlin() -> lines.add(".cookies(${CookieWriter.cookiesName(elc)})")
                    format.isJavaScript() -> lines.add(".set('Cookie', ${CookieWriter.cookiesName(elc)})")
                    // Python cookies are set alongside the headers and body when performing the request
                }
            }
        }
    }


    protected fun handleResponseAfterTheCall(
            call: HttpWsAction,
            res: HttpWsCallResult,
            responseVariableName: String,
            lines: Lines
    ) {

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

            format.isCsharp() -> {
                lines.add("Assert.Equal($code, (int) $responseVariableName.StatusCode);")
            }

            format.isPython() -> {
                lines.add("assert $responseVariableName.status_code == $code")
            }

            else -> {
                LoggingUtil.uniqueWarn(log, "No status assertion supported for format $format")
            }
        }

        handleLastStatementComment(res, lines)

        if (config.enableBasicAssertions && !call.shouldSkipAssertionsOnResponseBody()) {
            handleResponseAssertions(lines, res, responseVariableName)
        }
    }

    protected open fun handleLastStatementComment(res: HttpWsCallResult, lines: Lines) {
        val code = res.getStatusCode()
        if (code == 500 && !config.blackBox) {
            lines.appendSingleCommentLine(res.getLastStatementWhen500() ?: "")
        }
    }

    protected fun handleSingleCall(
        evaluatedAction: EvaluatedAction,
        index: Int,
        fv: FitnessValue,
        lines: Lines,
        testCaseName: String,
        testSuitePath: Path?,
        baseUrlOfSut: String
    ) {

        val exActions = mutableListOf<HttpExternalServiceAction>()
        // add all used external service actions for the action
        if (config.isEnabledExternalServiceMocking()) {
            if (evaluatedAction.action.parent !is EnterpriseActionGroup<*>)
                throw IllegalStateException("invalid parent of the RestAction, it is expected to be EnterpriseActionGroup, but it is ${evaluatedAction.action.parent!!::class.java.simpleName}")
            val group = evaluatedAction.action.parent as EnterpriseActionGroup<*>
            exActions.addAll(
                    group.getExternalServiceActions().filterIsInstance<HttpExternalServiceAction>()
                            .filter { it.active })

            if (exActions.isNotEmpty()) {
                if (format.isJavaOrKotlin()) {
                    handleExternalServiceActions(lines, exActions)
                } else {
                    log.warn("In mocking of external services, we do NOT support for other format ($format) except JavaOrKotlin")
                }
            }
        }

        lines.addEmpty()

        val call = evaluatedAction.action as HttpWsAction
        val res = evaluatedAction.result as HttpWsCallResult

        if (config.ssrf && res.getVulnerableForSSRF()) {
            handleSSRFFaultsPrologue(lines, call)
        }

        if (res.failedCall()) {
            addActionInTryCatch(call, index, testCaseName, lines, res, testSuitePath, baseUrlOfSut)
        } else {
            addActionLines(call, index, testCaseName, lines, res, testSuitePath, baseUrlOfSut)
        }

        if (config.ssrf && res.getVulnerableForSSRF()) {
            handleSSRFFaultsEpilogue(lines, call)
        }

        // reset all used external service action
        if (exActions.isNotEmpty()) {
            if (!format.isJavaOrKotlin()) {
                log.warn("NOT support for other format ($format) except JavaOrKotlin")
            } else {
                lines.addEmpty(1)
                exActions
                        .distinctBy { it.externalService.getSignature() }
                        .forEach { action ->
                            lines.add("${TestWriterUtils.getWireMockVariableName(action.externalService)}.resetAll()")
                            lines.appendSemicolon()
                        }
            }
        }
    }

    protected fun makeHttpCall(call: HttpWsAction, lines: Lines, res: HttpWsCallResult, baseUrlOfSut: String): String {
        //first handle the first line
        val responseVariableName = createUniqueResponseVariableName()

        if (format.isPython()) {
            handleHeaders(call, lines)
            handleBody(call, lines)
            lines.add(getAcceptHeader(call, res))
        }

        var dtoVar: String? = null
        if (config.dtoSupportedForPayload()) {
            dtoVar = writeDto(call, lines)
        }

        handleFirstLine(call, lines, res, responseVariableName)

        when {
            format.isJavaOrKotlin() -> {
                lines.indent(2)
                handleHeaders(call, lines)
                handleBody(call, lines, dtoVar)
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

            format.isPython() -> {
                lines.indent(2)
                handleVerbEndpoint(baseUrlOfSut, call, lines)
                lines.deindent(2)
            }
        }

        if (format.isJavaOrKotlin()) {
            handleResponseDirectlyInTheCall(call, res, lines)
        }
        handleLastLine(call, res, lines, responseVariableName)
        return responseVariableName
    }


    abstract fun handleVerbEndpoint(baseUrlOfSut: String, _call: HttpWsAction, lines: Lines)

    fun sendBodyCommand(): String {
        return when {
            format.isJavaOrKotlin() -> "body"
            format.isJavaScript() -> "send"
            format.isCsharp() -> ""
            format.isPython() -> ""
            else -> throw IllegalArgumentException("Format not supported $format")
        }
    }

    protected open fun handleBody(call: HttpWsAction, lines: Lines, dtoVar: String? = null) {

        val bodyParam = call.parameters.find { p -> p is BodyParam } as BodyParam?

        if (format.isCsharp() && bodyParam == null) {
            lines.append("null")
            return
        }

        if (bodyParam != null) {

            val send = sendBodyCommand()

            when {
                format.isJavaOrKotlin() -> lines.add(".contentType(\"${bodyParam.contentType()}\")")
                format.isJavaScript() -> lines.add(".set('Content-Type','${bodyParam.contentType()}')")
                format.isPython() -> lines.add("headers[\"content-type\"] = \"${bodyParam.contentType()}\"")
            }

            if (bodyParam.isJson()) {

                if (format.isPython()) {
                    lines.add("body = {}")
                }

                val json = bodyParam.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = format)

                printSendJsonBody(json, lines, dtoVar)

            } else if (bodyParam.isTextPlain()) {

                val body = bodyParam.getValueAsPrintableString(mode = GeneUtils.EscapeMode.TEXT, targetFormat = format)
                if (body != "\"\"") {
                    when {
                        format.isCsharp() -> {
                            lines.append("new StringContent(\"$body\", Encoding.UTF8, \"${bodyParam.contentType()}\")")
                        }
                        format.isPython() -> {
                            if (body.trim().isNullOrBlank()) {
                                lines.add("body = \"\"")
                            } else {
                                lines.add("body = $body")
                            }
                        }
                        else -> lines.add(".$send($body)")
                    }
                } else {
                    when {
                        format.isCsharp() -> {
                            lines.append("new StringContent(\"${"""\"\""""}\", Encoding.UTF8, \"${bodyParam.contentType()}\")")
                        }
                        format.isPython() -> {
                            lines.add("body = \"\"")
                        }
                        else -> lines.add(".$send(\"${"""\"\""""}\")")
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
                when {
                    format.isCsharp() -> {
                        lines.append("new StringContent(\"$body\", Encoding.UTF8, \"${bodyParam.contentType()}\")")
                    }
                    format.isPython() -> {
                        lines.add("body = \"$body\"")
                    }
                    else -> lines.add(".$send(\"$body\")")
                }
            } else if (bodyParam.isXml()) {

                val xml = bodyParam.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML, targetFormat = format)

                when {

                    format.isCsharp() -> {
                        lines.append("new StringContent($xml, Encoding.UTF8, \"${bodyParam.contentType()}\")")
                    }
                    format.isPython() -> {
                        lines.add("body = $xml")
                    }
                    else -> lines.add(".$send($xml)")
                }
            } else {
                LoggingUtil.uniqueWarn(log, "Unhandled type for body payload: " + bodyParam.contentType())
            }
        }
    }

    fun printSendJsonBody(json: String, lines: Lines, dtoVar: String? = null) {

        if(json.isEmpty()){
            //nothing is sent
            return
        }

        val send = sendBodyCommand()

        val bodyLines = formatJsonWithEscapes(json, format)

        if (bodyLines.size == 1) {
            when {
                format.isCsharp() -> {
                    lines.add("new StringContent(${bodyLines.first()}, Encoding.UTF8, \"application/json\")")
                }
                format.isPython() -> {
                    lines.add("body = ${bodyLines.first()}")
                }
                format.isJavaScript() -> writeStringifiedPayload(lines, send, bodyLines, false)
                else -> writeJavaOrKotlinJsonBody(lines, send, bodyLines, dtoVar, false)
            }
        } else {
            when {
                format.isCsharp() -> {
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
                format.isPython() -> {
                    lines.add("body = ${bodyLines.first()} + \\")
                    lines.indented {
                        (1 until bodyLines.lastIndex).forEach { i ->
                            lines.add("${bodyLines[i]} + \\")
                        }
                        lines.add("${bodyLines.last()}")
                    }
                }
                format.isJavaScript() -> writeStringifiedPayload(lines, send, bodyLines, true)
                else -> writeJavaOrKotlinJsonBody(lines, send, bodyLines, dtoVar, true)
            }
        }
    }

    private fun writeJavaOrKotlinJsonBody(lines: Lines, send: String, bodyLines: List<String>, dtoVar: String?, isMultiLine: Boolean) {
        // TODO: When performing robustness testing, we'll need to check the individual type and send data
        //  as stringified JSON instead of DTO, allowing for wrong payloads being tested
        if (shouldUseDtoForPayload(dtoVar)) {
            lines.add(".$send(${dtoVar})")
        } else {
            writeStringifiedPayload(lines, send, bodyLines, isMultiLine)
        }
    }

    private fun shouldUseDtoForPayload(dtoVar: String?): Boolean {
        return config.dtoSupportedForPayload() && dtoVar?.isNotEmpty() == true
    }

    private fun writeStringifiedPayload(lines: Lines, send: String, bodyLines: List<String>, isMultiLine: Boolean) {
        lines.add(".$send(${bodyLines.first()}")
        if (isMultiLine) {
            lines.append(" + ")
            lines.indented {
                (1 until bodyLines.lastIndex).forEach { i ->
                    lines.add("${bodyLines[i]} + ")
                }
                lines.add("${bodyLines.last()}")
            }
        }
        lines.append(")")
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

            if (config.enableBasicAssertions && !call.shouldSkipAssertionsOnResponseBody()) {
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

        if (res.getTooLargeBody()) {
            lines.addSingleCommentLine("the response payload was too large, above the threshold of ${config.maxResponseByteSize} bytes." +
                    " No assertion on it is therefore generated.")
            return
        }

        val bodyString = res.getBody()

        if (bodyString.isNullOrBlank()) {
            lines.add(emptyBodyCheck(responseVariableName))
            return
        }

        if (res.getBodyType() != null) {
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
                format.isPython() -> "assert \"$bodyTypeSimplified\" in $responseVariableName.headers[\"content-type\"]"
                else -> throw IllegalStateException("Unsupported format $format")
            }
            lines.add(instruction)
        }

        val type = res.getBodyType()
        // if there is payload, but no type identified, treat it as plain text
                ?: MediaType.TEXT_PLAIN_TYPE

        var bodyVarName = responseVariableName

        if (format.isCsharp()) {
            //cannot use response object directly, as need to unmarshall the body payload manually
            bodyVarName = createUniqueBodyVariableName()
            lines.add("dynamic $bodyVarName = ")
        }

        if (type.isCompatible(MediaType.APPLICATION_JSON_TYPE) || type.toString().lowercase().contains("+json")) {

            if (format.isCsharp()) {
                lines.append("JsonConvert.DeserializeObject(await $responseVariableName.Content.ReadAsStringAsync());")
            }

            handleJsonStringAssertion(bodyString, lines, bodyVarName, res.getTooLargeBody())

        } else if (type.isCompatible(MediaType.TEXT_PLAIN_TYPE)) {

            if (format.isCsharp()) {
                lines.append("await $responseVariableName.Content.ReadAsStringAsync();")
            }

            handleTextPlainTextAssertion(bodyString, lines, bodyVarName)
        } else {
            if (format.isCsharp()) {
                lines.append("await $responseVariableName.Content.ReadAsStringAsync();")
            }
            LoggingUtil.uniqueWarn(log, "Currently no assertions are generated for response type: $type")
        }
    }

    protected fun handlePythonVerbEndpoint(call: HttpWsAction, lines: Lines, appendBodyArgument: (HttpWsAction) -> Unit) {
        lines.append(",")
        lines.indented {
            lines.add("headers=headers")
            val elc = call.auth.endpointCallLogin
            if (elc != null && elc.expectsCookie()) {
                lines.append(", cookies=${CookieWriter.cookiesName(elc)}")
            }
            appendBodyArgument(call)
        }
    }

    protected fun handleLastLine(call: HttpWsAction, res: HttpWsCallResult, lines: Lines, resVarName: String) {

        if (format.isJavaScript()) {
            /*
                This is to deal with very weird behavior in SuperAgent that crashes the tests
                for status codes different from 2xx...
                so, here we make it passes as long as a status was present
             */
            lines.add(".ok(res => res.status)")
        }


        if (lines.shouldUseSemicolon()) {
            /*
                FIXME this is wrong when // is in a string of response, like a URL.
                Need to check last //, and that is not inside  ""
                Need tests for it... albeit tricky as Kotlin does not use ;, so need Java or unit test

                However, currently we have comments _only_ on status codes, which does not use ".
                so a dirty quick fix is to check if no " is used
             */
            if (lines.currentContains("//") && !lines.currentContains("\"")) {
                //a ; after a comment // would be ignored otherwise
                if (lines.isCurrentACommentLine()) {
                    //let's not lose indentation
                    lines.replaceInCurrent(Regex("//"), "; //")
                } else {
                    //there can be any number of spaces between the statement and the //
                    lines.replaceInCurrent(Regex("\\s*//"), "; //")
                }

            } else {
                lines.appendSemicolon()
            }
        }

        //TODO what was the reason for this?
        if (!format.isCsharp() && !format.isPython()) {
            lines.deindent(2)
        }
    }

    protected fun extractValueFromJsonResponse(resVarName: String, jsonPointer: String) : String{

        val extraTypeInfo = when {
            format.isKotlin() -> "<Object>"
            else -> ""
        }

        val jsonPath = JsonUtils.fromPointerToPath(jsonPointer)

        return when {
            format.isPython() -> "str($resVarName.json()${JsonUtils.fromPointerToDictionaryAccess(jsonPointer)})"
            format.isJavaScript() -> "$resVarName.body.$jsonPath.toString()"
            format.isJavaOrKotlin() -> "$resVarName.extract().body().path$extraTypeInfo(\"$jsonPath\").toString()"
            else -> throw IllegalStateException("Unsupported format $format")
        }
    }

    /**
     * Method to set up stub for HttpCallbackVerifier to the test case.
     */
    private fun handleSSRFFaultsPrologue(lines: Lines, action: Action) {
        val verifier = httpCallbackVerifier.getActionVerifierMapping(action.getName())

        if (verifier != null) {
            if (format.isJava()) {
                lines.addStatement("assertNotNull(${verifier.getVerifierName()}.isRunning())")
            }
            if (format.isKotlin()) {
                lines.addStatement("assertNotNull(${verifier.getVerifierName()}.isRunning)")
            }
            lines.addEmpty(1)

            //Reset verifier before test execution.
            lines.addStatement("${verifier.getVerifierName()}.resetAll()")

            lines.add("${verifier.getVerifierName()}.stubFor(")
            lines.indented {
                lines.add("get(\"${verifier.stub}\")")
                lines.indented {
                    lines.add(".withMetadata(Metadata.metadata().attr(\"ssrf\", \"${action.getName()}\"))")
                    lines.add(".atPriority(1)")
                    lines.add(".willReturn(")
                    lines.indented {
                        lines.add("aResponse()")
                        lines.indented {
                            lines.add(".withStatus(${HttpCallbackVerifier.SSRF_RESPONSE_STATUS_CODE})")
                            lines.add(".withBody(\"${HttpCallbackVerifier.SSRF_RESPONSE_BODY}\")")
                        }
                    }
                    lines.add(")")
                }
            }
            lines.addStatement(")")
            lines.addEmpty(1)
            handleCallbackVerifierRequests(lines, action, verifier, false)
            lines.addEmpty(1)
        }
    }

    private fun handleSSRFFaultsEpilogue(lines: Lines, action: Action) {
        val verifier = httpCallbackVerifier.getActionVerifierMapping(action.getName())

        if (verifier != null) {
            lines.addEmpty(1)
            handleCallbackVerifierRequests(lines, action, verifier, true)
        }
    }

    private fun handleCallbackVerifierRequests(lines: Lines, action: Action, verifier: ActionStubMapping, assertTrue: Boolean) {
        if (assertTrue) {
            lines.addSingleCommentLine("Verifying that the request is successfully made to HttpCallbackVerifier after test execution.")
            lines.add("assertTrue(${verifier.getVerifierName()}")
        } else {
            lines.addSingleCommentLine("Verifying that there are no requests made to HttpCallbackVerifier before test execution.")
            lines.add("assertFalse(${verifier.getVerifierName()}")
        }
        lines.indented {
            if (format.isKotlin()) {
                lines.add(".allServeEvents")
                lines.add(".filter { it.wasMatched && it.stubMapping.metadata != null }")
                lines.add(".any { it.stubMapping.metadata.getString(\"ssrf\") == \"${action.getName()}\" }")
            }
        }
        lines.add(")")
    }

}
