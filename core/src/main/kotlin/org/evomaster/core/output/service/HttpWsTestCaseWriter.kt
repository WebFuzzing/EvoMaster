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
import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.EnterpriseActionResult
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
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
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchDocumentGene
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

    /**
     * For a HTTP_TIMEOUT fault, the call is expected to fail, and the assertion is expressed
     * directly on the call:
     *  - JS:     await expect(...).rejects.toThrow()
     *  - Python: with self.assertRaises(Exception): ...
     */
    protected fun hasTimeoutFault(res: ActionResult): Boolean {
        return res is EnterpriseActionResult
                && res.getFaults().any { it.category == ExperimentalFaultCategory.HTTP_TIMEOUT }
    }

    protected fun expectsRejection(res: ActionResult) = format.isJavaScript() && !format.isPlaywright() && hasTimeoutFault(res)

    protected fun expectsAssertRaises(res: ActionResult) = format.isPython() && hasTimeoutFault(res)

    fun startRequest(lines: Lines){
        when {
            format.isJavaOrKotlin() -> lines.append("given()")
            format.isPlaywright() -> lines.append("await request")
            format.isJavaScript() && !format.isPlaywright() -> lines.append("await superagent")
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

        if (needsResponseVariable(call, res) && !res.invalidCall()) {
            when {
                format.isKotlin() -> lines.append("val $resVarName: ValidatableResponse = ")
                format.isJava() -> lines.append("ValidatableResponse $resVarName = ")
                format.isJavaScript() || format.isPlaywright() -> lines.append("const $resVarName = ")
                format.isPython() -> lines.append("$resVarName = ")
                format.isCsharp() -> lines.append("var $resVarName = ")
            }
        }

        when {
            format.isJavaOrKotlin() -> lines.append("given()")
            // for a call expected to reject, wrap it in await expect(...).rejects.toThrow()
            // For Playwright, the verb handler will write `await request.<verb>(...)`
            format.isJavaScript() && !format.isPlaywright() -> lines.append(if (expectsRejection(res)) "await expect(superagent" else "await superagent")
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
        if (bodyParam != null && bodyParam.isJson() && payloadIsValidJson(bodyParam)) {
            val primaryGene = bodyParam.primaryGene()
            if (primaryGene.getWrappedGene(JsonPatchDocumentGene::class.java) != null) {
                return ""
            }
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
                if (leafGene is ObjectGene || leafGene is ArrayGene<*> || leafGene is FixedMapGene<*,*>) {
                    return generateDtoCall(leafGene, actionName, lines).varName
                }
            }

        }
        return ""
    }

    /*
     * Control characters break JSON and transform it into an invalid payload. If there's any invalid character
     * then we'll avoid using DTOs and have the payload in the test case be represented by the raw JSON string.
     */
    private fun payloadIsValidJson(bodyParam: BodyParam): Boolean {
        val json = bodyParam.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = format)
        return OutputFormatter.JSON_FORMATTER.isValid(json)
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
            // For Playwright, headers are expressed as entries in an object literal
            format.isPlaywright() -> "'Accept': "
            format.isJavaScript() -> ".set('Accept', "
            format.isCsharp() -> "Client.DefaultRequestHeaders.Add(\"Accept\", "
            format.isPython() -> "headers['Accept'] = "
            else -> throw IllegalArgumentException("Invalid format: $format")
        }
    }

    protected fun closeAcceptHeader(openedHeader: String): String {
        var result = openedHeader
        // Do not append a closing parenthesis for Playwright as we are not using a method call
        if (!config.outputFormat.isPython() && !format.isPlaywright()) {
            result += ")"
        }
        if (format.isCsharp()){
            result = "$result;"
        }
        if (format.isPlaywright()) { result += ","
        }
        return result
    }


    open fun needsResponseVariable(call: HttpWsAction, res: HttpWsCallResult): Boolean {
        /*
          Bit tricky... when using RestAssured on JVM, we can assert directly on the call...
          but that is not the case for the other libraries used for example in JS and C#
         */
        return config.enableBasicAssertions &&
                (config.outputFormat == OutputFormat.JS_JEST || config.outputFormat == OutputFormat.JS_PLAYWRIGHT || config.outputFormat == OutputFormat.PYTHON_UNITTEST)
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
            format.isJavaScript() && !format.isPlaywright()-> "set"
            format.isPlaywright() -> "" // headers are handled in a map in the options object
            format.isPython() -> "headers = {}"
            else -> throw IllegalArgumentException("Not supported format: $format")
        }

        if (format.isPython()) {
            lines.add(set)
        }

        val bodyParam = call.parameters.find { p -> p is BodyParam } as BodyParam?

        if (format.isPlaywright() && bodyParam != null) {
            val contentType = bodyParam.contentType()
            if (contentType != null) {
                lines.add("'Content-Type': '$contentType',")
            }
        }

        //headers in specified auth info
        call.auth.headers.forEach {
            if (format.isPython()) {
                lines.add("headers[\"${it.name}\"] = \"${it.value}\"")
            } else if (format.isPlaywright()) {
                lines.add("'${it.name}': '${it.value}', // ${call.auth.name}")
            } else {
                lines.add(".$set(\"${it.name}\", \"${it.value}\") // ${call.auth.name}")
            }
        }

        val elc = call.auth.endpointCallLogin

        //headers from schema
        call.parameters.filterIsInstance<HeaderParam>()
            .filter { !prechosenAuthHeaders.contains(it.name) }
            .filter { elc?.token == null || !(it.name.equals(elc.token.sendName, true)) }
            .filter { it.isInUse() }
            .forEach {
                val x = it.getRawValue()
                val escapedHeader = GeneUtils.applyEscapes(x, GeneUtils.EscapeMode.BODY, format)
                if (format.isPython()) {
                    lines.add("headers[\"${it.name}\"] = \"${escapedHeader}\"")
                } else if (format.isPlaywright()) {
                        lines.add("'${it.name}': '${escapedHeader}',")
                } else {

                    lines.add(".$set(\"${it.name}\", \"${escapedHeader}\")")
                }
            }

        if (elc != null) {

            if (!elc.expectsCookie()) {
                //TODO should check for sendIn
                val tokenHeader = elc.token!!.sendName
                if (format.isPython()) {
                    lines.add("headers[\"$tokenHeader\"] = ${TokenWriter.authPayloadName(elc)} # ${call.auth.name}")
                } else if (format.isPlaywright()) {
                    lines.add("'$tokenHeader': ${TokenWriter.authPayloadName(elc)}, // ${call.auth.name}")
                } else {
                    lines.add(".$set(\"$tokenHeader\", ${TokenWriter.authPayloadName(elc)}) // ${call.auth.name}")
                }
            } else {
                when {
                    format.isJavaOrKotlin() -> lines.add(".cookies(${CookieWriter.cookiesName(elc)})")
                    format.isJavaScript() -> when {
                        format.isPlaywright() -> {
                            val cookieVar = CookieWriter.cookiesName(elc)
                        lines.add("'Cookie': $cookieVar,")
                        }
                        else ->
                            lines.add(".set('Cookie', ${CookieWriter.cookiesName(elc)})")
                    }
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
                || res.invalidCall()
        ) {
            return
        }

        lines.addEmpty()

        val code = res.getStatusCode()

        when {

            format.isJavaScript() && format.isPlaywright() -> {
                val statusAssert = "expect($responseVariableName.status()).toBe($code);"
                if (res.getFlakyStatusCode() == null){
                    lines.add(statusAssert)
                }else{
                    lines.addSingleCommentLine(flakyInfo("Status Code", code.toString(), res.getFlakyStatusCode().toString()))
                    lines.addSingleCommentLine(statusAssert)
                }
                lines.addEmpty()
            }

            format.isJavaScript() && !format.isPlaywright() -> {
                val statusAssert = "expect($responseVariableName.status).toBe($code);"
                if (res.getFlakyStatusCode() == null){
                    lines.add(statusAssert)
                }else{
                    lines.addSingleCommentLine(flakyInfo("Status Code", code.toString(), res.getFlakyStatusCode().toString()))
                    lines.addSingleCommentLine(statusAssert)
                }
            }

            format.isCsharp() -> {
                lines.add("Assert.Equal($code, (int) $responseVariableName.StatusCode);")
            }

            format.isPython() -> {
                val statusAssert = "assert $responseVariableName.status_code == $code"
                if (res.getFlakyStatusCode() == null){
                    lines.add(statusAssert)
                }else{
                    lines.addSingleCommentLine(flakyInfo("Status Code", code.toString(), res.getFlakyStatusCode().toString()))
                    lines.addSingleCommentLine(statusAssert)
                }
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
        baseUrlOfSut: String,
        addTimeMeasurement: Boolean,
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

        var timeStartName = ""

        if(addTimeMeasurement)
        {
            timeStartName = handleExecutionTimePrologue(lines);
        }

        if (res.invalidCall() && !expectsRejection(res) && !expectsAssertRaises(res)) {
            addActionInTryCatch(call, index, testCaseName, lines, res, testSuitePath, baseUrlOfSut)
        } else {
            addActionLines(call, index, testCaseName, lines, res, testSuitePath, baseUrlOfSut)
        }

        if(addTimeMeasurement)
        {
            handleExecutionTimeEpilogue(lines, timeStartName, res.getVulnerableForSQLI())
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


    private fun handleExecutionTimePrologue(lines: Lines):String {

        var varName = createUniqueResponseVariableName() + "_ms"
        lines.addEmpty(1)
        lines.addSingleCommentLine("$varName stores the start time in milliseconds")

        if(format.isJava()){
            lines.addStatement("long $varName = System.currentTimeMillis()")
        } else if (format.isKotlin()) {
            lines.addStatement("val $varName = System.currentTimeMillis()")
        } else if(format.isPython()) {
            lines.addStatement("$varName = time.perf_counter() * 1000")
        } else if(format.isPlaywright()) {
            lines.addStatement("const $varName = performance.now()")
        } else if(format.isJavaScript()) {
            lines.addStatement("$varName = performance.now()")
        }
        lines.addEmpty(1)

        return varName
    }

    private fun handleExecutionTimeEpilogue(lines: Lines, varName: String, isVulnerable: Boolean) {
        var finalVarName = createUniqueResponseVariableName() + "_ms"
        lines.addEmpty(1)

        lines.addSingleCommentLine("$finalVarName stores the total execution time in milliseconds")
        if(format.isJava()){
            lines.addStatement("long $finalVarName = System.currentTimeMillis() - $varName")
        } else if (format.isKotlin()) {
            lines.addStatement("val $finalVarName = System.currentTimeMillis() - $varName")
        } else if(format.isPython()) {
            lines.addStatement("$finalVarName = (time.perf_counter() * 1000) - $varName")
        } else if(format.isPlaywright()) {
            lines.addStatement("const $finalVarName = performance.now() - $varName")
        } else if(format.isJavaScript()) {
            lines.addStatement("$finalVarName = performance.now() - $varName")
        }

        lines.addEmpty(1)

        if(isVulnerable)
        {
            lines.addSingleCommentLine("Note: SQL Injection vulnerability detected in this call. Expected response time (sqliInjectedSleepDurationMs) should be greater than ${config.sqliInjectedSleepDurationMs} ms.")
            when{
                format.isJavaOrKotlin() -> lines.addStatement("assertTrue($finalVarName > ${config.sqliInjectedSleepDurationMs})")
                format.isPlaywright() -> lines.addStatement("expect($finalVarName).toBeGreaterThan(${config.sqliInjectedSleepDurationMs})")
                format.isJavaScript() -> lines.addStatement("expect($finalVarName).toBeGreaterThan(${config.sqliInjectedSleepDurationMs})")
                format.isPython() -> lines.addStatement("assert $finalVarName > ${config.sqliInjectedSleepDurationMs}")
                else -> {}
            }
        }else {
            lines.addSingleCommentLine("Note: No SQL Injection vulnerability detected in this call. Expected response time (sqliBaselineMaxResponseTimeMs) should be less than ${config.sqliBaselineMaxResponseTimeMs} ms.")
            when{
                format.isJavaOrKotlin() -> lines.addStatement("assertTrue($finalVarName < ${config.sqliBaselineMaxResponseTimeMs})")
                format.isPlaywright() -> lines.addStatement("expect($finalVarName).toBeLessThan(${config.sqliBaselineMaxResponseTimeMs})")
                format.isJavaScript()-> lines.addStatement("expect($finalVarName).toBeLessThan(${config.sqliBaselineMaxResponseTimeMs})")
                format.isPython() -> lines.addStatement("assert $finalVarName < ${config.sqliBaselineMaxResponseTimeMs}")
                else -> {}
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

        val pyAssertRaises = expectsAssertRaises(res)
        if (pyAssertRaises) {
            lines.add("with self.assertRaises(Exception):")
            lines.indent()
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
                if (format.isPlaywright()) {
                    handleVerbEndpoint(baseUrlOfSut, call, lines)
                    lines.replaceInCurrent(Regex("\\)$"), "")
                    lines.append(", {")
                    lines.addEmpty()
                    lines.indented {
                        if (call is org.evomaster.core.problem.rest.data.RestCallAction) {
                            lines.add("method: \"${call.verb.name.uppercase()}\",")
                        }
                        lines.add("headers: {")
                        lines.indented {
                            lines.add(getAcceptHeader(call, res))
                            handleHeaders(call, lines)
                        }
                        lines.add("},")
                        handleBody(call, lines, dtoVar)
                        lines.add("maxRedirects: 0,")
                        lines.add("ignoreHTTPSErrors: true,")
                    }
                    lines.add("})")
                } else {
                //in SuperAgent, verb must be first
                handleVerbEndpoint(baseUrlOfSut, call, lines)
                //client timeout, same source as fuzzing tcpTimeoutMs
                lines.add(".timeout({response: ${TestSuiteWriter.httpTimeoutVarMs}, deadline: ${TestSuiteWriter.httpTimeoutVarMs}})")
                lines.append(getAcceptHeader(call, res))
                handleHeaders(call, lines)
                handleBody(call, lines)
            }
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

        if (pyAssertRaises) {
            lines.deindent()
        }
        return responseVariableName
    }


    abstract fun handleVerbEndpoint(baseUrlOfSut: String, _call: HttpWsAction, lines: Lines)

    fun sendBodyCommand(): String {
        return when {
            format.isJavaOrKotlin() -> "body"
            format.isJavaScript() && !format.isPlaywright() -> "send"
            format.isPlaywright() -> "data"
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

        if (bodyParam == null) {

            if(call is RestCallAction && call.verb == HttpVerb.POST && format.isJavaOrKotlin()){
                //   RestAssured automatically add content-type for forms on POST without body :(
                lines.add(".noContentType()")
            }

            return
        }

        val send = sendBodyCommand()

        when {
            format.isJavaOrKotlin() -> lines.add(".contentType(\"${bodyParam.contentType()}\")")
            format.isJavaScript() && !format.isPlaywright() -> lines.add(".set('Content-Type','${bodyParam.contentType()}')")
            format.isPlaywright() -> {
                // handled in makeHttpCall through options object
            }
            format.isPython() -> lines.add("headers[\"content-type\"] = \"${bodyParam.contentType()}\"")
        }

        if (bodyParam.isJson()) {

            if (format.isPython()) {
                lines.add("body = {}")
            }

            val json = bodyParam.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = format)

            printSendJsonBody(json, lines, dtoVar)

        } else if (bodyParam.isTextPlain()) {

            handleTextBody(bodyParam, lines)

        } else if (bodyParam.isForm()) {
            val body = bodyParam.gene.getValueAsPrintableString(
                mode = GeneUtils.EscapeMode.X_WWW_FORM_URLENCODED,
                targetFormat = format
            )
            when {
                format.isPython() -> {
                    lines.add("body = \"$body\"")
                }
                format.isPlaywright() -> {
                    // In Playwright, use the options object field instead of chained calls
                    lines.add("data: \"$body\",")
                }
                else -> lines.add(".$send(\"$body\")")
            }
        } else if (bodyParam.isXml()) {

            val xml = bodyParam.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML, targetFormat = format)
            // Escape quotes for string literal in generated code
            val escapedXml = xml.replace("\\", "\\\\").replace("\"", "\\\"")

            when {
                format.isPython() -> {
                    lines.add("body = \"$escapedXml\"")
                }
                format.isPlaywright() -> {
                    lines.add("data: \"$escapedXml\",")
                }
                else -> lines.add(".$send(\"$escapedXml\")")
            }
        } else {
            LoggingUtil.uniqueWarn(log, "Unhandled type for body payload: " + bodyParam.contentType() +
                    ". It will be handled as TEXT")
            handleTextBody(bodyParam, lines)
        }

    }

    private fun handleTextBody(
        bodyParam: BodyParam,
        lines: Lines,
    ) {
        val send = sendBodyCommand()

        val body = bodyParam.getValueAsPrintableString(mode = GeneUtils.EscapeMode.TEXT, targetFormat = format)

        val text = GeneUtils.applyEscapes(body, mode = GeneUtils.EscapeMode.TEXT, format = format)

        // handle body only if it is not black
        if (body.isNotBlank()) {
            if (body != "\"\"") {
                when {
                    format.isPython() -> {
                        if (body.trim().isBlank()) {
                            lines.add("body = \"\"")
                        } else {
                            lines.add("body = \"$text\"")
                        }
                    }
                    format.isPlaywright() -> {
                        // In Playwright, body data must be inside the request options object
                        lines.add("data: \"$text\",")
                    }
                    else -> lines.add(".$send(\"$text\")")
                }
            } else {
                when {
                    format.isPython() -> {
                        lines.add("body = \"\"")
                    }
                    //TODO isn't this valid just for Kotlin???
                    format.isPlaywright() -> {
                        // Empty body for Playwright should still be provided in options when needed
                        lines.add("data: \"\",")
                    }
                    else -> lines.add(".$send(\"${"""\"\""""}\")")
                }
            }
        }
    }

    /**
     * @param json the representation to send
     * @param dtoVar whether we rather send the data as DTO, stored in a variable with this name
     * @param functionsOnString  appended function calls on the string representation before sending it
     */
    fun printSendJsonBody(
        json: String,
        lines: Lines,
        dtoVar: String? = null,
        functionsOnString: List<String>? = null
    ) {

        if(json.isEmpty()){
            //nothing is sent
            return
        }

        // For Playwright, if a DTO variable is available and allowed, send it directly in the options object
        if (format.isPlaywright() && shouldUseDtoForPayload(dtoVar)) {
            lines.add("data: ${dtoVar},")
            return
        }

        if(dtoVar != null && functionsOnString != null) {
            throw IllegalArgumentException("Cannot use extra functions on string JSON when using DTOs")
        }

        val send = sendBodyCommand()

        val bodyLines = formatJsonWithEscapes(json, format)

        if (bodyLines.size == 1) {
            when {
                format.isPython() -> {
                    lines.add("body = ${bodyLines.first()}")
                    functionsOnString?.forEach { lines.append(it) }
                }
                format.isPlaywright() -> {
                    writePlaywrightPayload(lines, bodyLines, false, functionsOnString)
                }
                format.isJavaScript() -> writeStringifiedPayload(lines, send, bodyLines, functionsOnString)
                else -> writeJavaOrKotlinJsonBody(lines, send, bodyLines, dtoVar, functionsOnString)
            }
        } else {
            when {
                format.isPython() -> {
                    lines.add("body = ")
                    if(!functionsOnString.isNullOrEmpty()){
                        lines.append("(")
                    }
                    lines.append("${bodyLines.first()} + \\")
                    lines.indented {
                        (1 until bodyLines.lastIndex).forEach { i ->
                            lines.add("${bodyLines[i]} + \\")
                        }
                        lines.add(bodyLines.last())
                        if(!functionsOnString.isNullOrEmpty()){
                            lines.append(")")
                            functionsOnString.forEach { lines.append(it) }
                        }
                    }
                }
                format.isPlaywright() -> writePlaywrightPayload(lines, bodyLines, true, functionsOnString)
                format.isJavaScript() -> writeStringifiedPayload(lines, send, bodyLines, functionsOnString)
                else -> writeJavaOrKotlinJsonBody(lines, send, bodyLines, dtoVar, functionsOnString)
            }
        }
    }

    private fun writeJavaOrKotlinJsonBody(
        lines: Lines,
        send: String,
        bodyLines: List<String>,
        dtoVar: String?,
        functionsOnString: List<String>?
    ) {
        // TODO: When performing robustness testing, we'll need to check the individual type and send data
        //  as stringified JSON instead of DTO, allowing for wrong payloads being tested
        if (shouldUseDtoForPayload(dtoVar)) {
            lines.add(".$send(${dtoVar})")
        } else {
            writeStringifiedPayload(lines, send, bodyLines, functionsOnString)
        }
    }

    private fun shouldUseDtoForPayload(dtoVar: String?): Boolean {
        return config.dtoSupportedForPayload() && dtoVar?.isNotEmpty() == true
    }

    private fun writeStringifiedPayload(
        lines: Lines,
        send: String,
        bodyLines: List<String>,
        functionsOnString: List<String>?
    ) {
        if(bodyLines.isEmpty()) {
            throw IllegalArgumentException("Empty JSON payload")
        }

        lines.add(".$send(")

        if(!functionsOnString.isNullOrEmpty() && bodyLines.size > 1) {
            //need to wrap string concatenation into a () to be able to call methods
            //on the final result
            lines.append("(")
        }

        lines.append(bodyLines.first())

        if(!functionsOnString.isNullOrEmpty() && bodyLines.size == 1) {
            //there is only 1 string, so no need for (), and can append directly
            functionsOnString.forEach {lines.append(it)}
        }

        if (bodyLines.size > 1) {
            lines.append(" + ")
            lines.indented {
                (1 until bodyLines.lastIndex).forEach { i ->
                    lines.add("${bodyLines[i]} + ")
                }
                lines.add(bodyLines.last())
            }
        }

        if(!functionsOnString.isNullOrEmpty() && bodyLines.size > 1) {
            lines.append(")")
            lines.indented {
                functionsOnString.forEach { lines.add(it) }
            }
        }

        lines.append(")")
    }

    private fun writePlaywrightPayload(
        lines: Lines,
        bodyLines: List<String>,
        isMultiLine: Boolean,
        functionsOnString: List<String>? = null
    ) {
        // Build the JSON string expression first
        lines.add("data: JSON.parse(${bodyLines.first()}")

        // If multi-line, concatenate subsequent lines
        if (isMultiLine) {
            lines.append(" + ")
            lines.indented {
                (1 until bodyLines.lastIndex).forEach { i ->
                    lines.add("${bodyLines[i]} + ")
                }
                lines.add("${bodyLines.last()}")
            }
        }

        // Apply any string transformation functions (e.g., .replace(...))
        functionsOnString?.let { funcs ->
            if (funcs.isNotEmpty()) {
                // If we built a multi-line concatenation, ensure method calls are appended correctly
                if (!isMultiLine) {
                    // single-line: can simply append functions inline
                    funcs.forEach { lines.append(it) }
                } else {
                    // multi-line: add functions each on its own line for readability
                    lines.indented {
                        funcs.forEach { lines.add(it) }
                    }
                }
            }
        }

        // Close JSON.parse(...), then the options object entry
        lines.append(")",
        )
        lines.append(",")
    }

    /**
     * This is done mainly for RestAssured
     */
    protected fun handleResponseDirectlyInTheCall(call: HttpWsAction, res: HttpWsCallResult, lines: Lines) {
        if (!res.invalidCall()) {

            val code = res.getStatusCode()

            when {
                format.isJavaOrKotlin() -> {
                    lines.add(".then()")
                    if (res.getFlakyStatusCode() == null) {
                        lines.add(".statusCode($code)")
                    } else {
                        lines.addSingleCommentLine(flakyInfo("Status Code", code.toString(), res.getFlakyStatusCode().toString()))
                        lines.addSingleCommentLine(".statusCode($code)")
                    }
                }

                format.isPlaywright() -> {
                    // assertions for Playwright are handled in handleResponseAfterTheCall,
                    // as they cannot be chained directly in the request call
                }

                else -> throw IllegalStateException("No assertion in calls for format: $format")
            }

            handleLastStatementComment(res, lines)

            if (config.enableBasicAssertions && !call.shouldSkipAssertionsOnResponseBody()) {
                handleResponseAssertions(lines, res, null)
            }
        }
    }

    //----------------------------------------------------------------------------------------
    // assertion lines

    protected fun addHeaderAssertions(lines: Lines, res: HttpWsCallResult, responseVariableName: String?){

        val status = res.getStatusCode()

        //TODO: verb order in Allow header is flaky
        val allow = res.getAllow() //could had rather checked if was OPTIONS, but we don't have that info as input here
        if(!allow.isNullOrBlank() || status == 405){
            addAssertionOnHeader(lines, "allow", res.getHeader("allow"), true, responseVariableName)
        }
        if(status == 401){
            addAssertionOnHeader(lines, "www-authenticate", res.getHeader("www-authenticate"), false, responseVariableName)
        }
        if(status == 426) {
            addAssertionOnHeader(lines, "upgrade", res.getHeader("upgrade"), false, responseVariableName)
        }
    }

    protected fun addAssertionOnHeader(lines: Lines, name: String, value: String?, flaky: Boolean, responseVariableName: String?){

        val instruction =
            if(value != null) {
                val escaped = GeneUtils.applyEscapes(value, GeneUtils.EscapeMode.ASSERTION, format)
                when {
                    format.isJavaOrKotlin() -> ".header(\"$name\", \"$escaped\")"
                    format.isPlaywright() ->
                        "expect($responseVariableName.headers()[\"$name\"]?.startsWith(\"$escaped\")).toBe(true);"
                    format.isJavaScript() ->
                        "expect($responseVariableName.header[\"$name\"].startsWith(\"$escaped\")).toBe(true);"
                    format.isPython() -> "assert \"$escaped\" in $responseVariableName.headers[\"$name\"]"
                    else -> throw IllegalStateException("Unsupported format $format")
                }
            } else {
                when {
                    format.isJavaOrKotlin() -> ".header(\"$name\", isEmptyOrNullString())"
                    format.isPlaywright() ->
                        "expect($responseVariableName.headers()[\"$name\"]).toBeUndefined();"
                    format.isJavaScript() ->
                        "expect($responseVariableName.header[\"$name\"]).toBeUndefined();"
                    format.isPython() -> "assert \"$name\" not in $responseVariableName.headers"
                    else -> throw IllegalStateException("Unsupported format $format")
                }
            }

        if(flaky) {
            lines.addSingleCommentLine(instruction)
        } else {
            lines.add(instruction)
        }
    }

    protected fun handleResponseAssertions(lines: Lines, res: HttpWsCallResult, responseVariableName: String?) {

        assert(responseVariableName != null || format.isJavaOrKotlin() || format.isPlaywright())

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

        addHeaderAssertions(lines, res, responseVariableName)

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
                format.isPlaywright() -> "expect($responseVariableName.headers()[\"content-type\"]).toContain(\"$bodyTypeSimplified\")"
                format.isJavaScript() ->
                    "expect($responseVariableName.header[\"content-type\"].startsWith(\"$bodyTypeSimplified\")).toBe(true);"

                format.isCsharp() -> "Assert.Contains(\"$bodyTypeSimplified\", $responseVariableName.Content.Headers.GetValues(\"Content-Type\").First());"
                format.isPython() -> "assert \"$bodyTypeSimplified\" in $responseVariableName.headers[\"content-type\"]"
                else -> throw IllegalStateException("Unsupported format $format")
            }

            // handle flaky body type
            if (res.getFlakyBodyType() == null){
                lines.add(instruction)
            } else{
                lines.addSingleCommentLine(instruction)
            }
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

            handleJsonStringAssertion(bodyString, res.getFlakyBodies()?.let { res.getMergedFlakyBody() }, lines, bodyVarName, res.getTooLargeBody())

        } else if (type.isCompatible(MediaType.TEXT_PLAIN_TYPE)) {

            if (format.isCsharp()) {
                lines.append("await $responseVariableName.Content.ReadAsStringAsync();")
            }

            handleTextPlainTextAssertion(bodyString, res.getFlakyBodies()?.let { res.getMergedFlakyBody() }, lines, bodyVarName)
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
        lines.append(", verify=False")
        }
    }

    protected fun handleLastLine(call: HttpWsAction, res: HttpWsCallResult, lines: Lines, resVarName: String) {
        if (format.isJavaScript() && !format.isPlaywright()) {
            /*
                This is to deal with very weird behavior in SuperAgent that crashes the tests
                for status codes different from 2xx...
                so, here we make it passes as long as a status was present
             */
            lines.add(".ok(res => res.status)")
            if (expectsRejection(res)) {
                //close the await expect(...) and assert the call rejected
                lines.append(").rejects.toThrow()")
            }
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

            } else if (config.handleFlakiness && lines.isCurrentACommentLine()){
                /*
                    regex:
                    Matches '//' only when it is immediately preceded by a whitespace character.
                 */
                lines.replaceFirstInCurrent(Regex("(?<=\\s)//"), "; //")
            }else {
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
        val dictAccess = JsonUtils.fromPointerToDictionaryAccess(jsonPointer)

        return when {
            format.isPython() -> "str($resVarName.json()${JsonUtils.fromPointerToDictionaryAccess(jsonPointer)})"
            format.isPlaywright() -> " ((await $resVarName.json())$dictAccess)?.toString()"
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
                    lines.add(".withMetadata(Metadata.metadata().attr(SSRF_METADATA_TAG, \"${action.getName()}\"))")
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
        val verifierHasReceivedRequestsCheck = "verifierHasReceivedRequests(${verifier.getVerifierName()}, \"${action.getName()}\")"
        if (assertTrue) {
            lines.addSingleCommentLine("Verifying that the request is successfully made to HttpCallbackVerifier after test execution.")
            lines.addStatement("assertTrue($verifierHasReceivedRequestsCheck)")
        } else {
            lines.addSingleCommentLine("Verifying that there are no requests made to HttpCallbackVerifier before test execution.")
            lines.addStatement("assertFalse($verifierHasReceivedRequestsCheck)")
        }
    }

}