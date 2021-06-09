package org.evomaster.core.output.service

import com.google.gson.Gson
import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.Lines
import org.evomaster.core.output.SqlWriter
import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.problem.httpws.service.HttpWsAction
import org.evomaster.core.problem.httpws.service.HttpWsCallResult
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.*
import org.evomaster.core.search.gene.GeneUtils
import org.slf4j.LoggerFactory
import javax.ws.rs.core.MediaType

class RestTestCaseWriter : HttpWsTestCaseWriter {

    companion object{
        private val log = LoggerFactory.getLogger(RestTestCaseWriter::class.java)
    }

    @Inject
    private lateinit var partialOracles : PartialOracles

    constructor() : super()

    /**
     * ONLY for tests
     */
    constructor(config: EMConfig, partialOracles: PartialOracles) : super(){
        this.config = config
        this.partialOracles = partialOracles
    }


    /**
     * When we make a HTTP call, do we need to store the response in a variable for following HTTP calls
     * or to create assertions on it?
     */
    override fun needsResponseVariable(call: HttpWsAction, res: HttpWsCallResult): Boolean {

        return super.needsResponseVariable(call, res) ||
                (config.expectationsActive && partialOracles.generatesExpectation(call as RestCallAction, res))
                || ((call as RestCallAction).saveLocation && !res.stopping)
    }

    override fun handleFieldDeclarations(lines: Lines, baseUrlOfSut: String, ind: EvaluatedIndividual<*>) {
        super.handleFieldDeclarations(lines, baseUrlOfSut, ind)

        if (shouldCheckExpectations()) {
            addDeclarationsForExpectations(lines, ind as EvaluatedIndividual<RestIndividual>)
            //TODO: -> also check expectation generation before adding declarations
        }

        if (hasChainedLocations(ind.individual)) {
            assert(ind.individual is RestIndividual)
            /*
                If the "location" header of a HTTP response is used in a following
                call, we need to save it in a variable.
                We declare all such variables at the beginning of the test.

                TODO: rather declare variable first time we access it?
             */
            lines.addEmpty()

            ind.evaluatedActions().asSequence()
                    .map { it.action }
                    .filterIsInstance(RestCallAction::class.java)
                    .filter { it.locationId != null }
                    .map { it.locationId }
                    .distinct()
                    .forEach { id ->
                        val name = locationVar(id!!)
                        when {
                            format.isJava() -> lines.add("String $name = \"\";")
                            format.isKotlin() -> lines.add("var $name : String? = \"\"")
                            format.isJavaScript() -> lines.add("let $name = \"\";")
                            format.isCsharp() -> lines.add("var $name = \"\";")
                                // should never happen
                            else -> throw IllegalStateException("Unsupported format $format")
                        }
                    }
        }
    }

    override fun handleActionCalls(lines: Lines, baseUrlOfSut: String, ind: EvaluatedIndividual<*>){
        //SQL actions are generated in between
        if (ind.individual is RestIndividual && config.isEnabledSQLInBetween()) {

            ind.evaluatedResourceActions().forEachIndexed { index, c ->
                // db
                if (c.first.isNotEmpty())
                    SqlWriter.handleDbInitialization(format, c.first, lines, ind.individual.seeDbActions(), groupIndex = index.toString())
                //actions
                c.second.forEach { a ->
                    handleRestCall(a, lines, baseUrlOfSut)
                }
            }
        } else {
            if (ind.individual is RestIndividual) {
                ind.evaluatedActions().forEach { a ->
                    handleRestCall(a, lines, baseUrlOfSut)
                }
            }
        }
    }

    protected fun locationVar(id: String): String {
        //TODO make sure name is syntactically valid
        //TODO use counters to make them unique
        return "location_${id.trim().replace(" ", "_")}"
    }


    /**
     * Check if any action requires a chain based on location headers:
     * eg a POST followed by a GET on the created resource
     */
    private fun hasChainedLocations(individual: Individual) : Boolean{
        return individual.seeActions().any { a ->
            a is RestCallAction && a.isLocationChained()
        }
    }


    override fun addActionLines(action: Action, lines: Lines, result: ActionResult, baseUrlOfSut: String){
        addRestCallLines(action as RestCallAction, lines, result as RestCallResult, baseUrlOfSut)
    }

    private fun handleRestCall(
            evaluatedAction: EvaluatedAction,
            lines: Lines,
            baseUrlOfSut: String
    ) {
        lines.addEmpty()

        val call = evaluatedAction.action as RestCallAction
        val res = evaluatedAction.result as RestCallResult

        if (res.failedCall()) {
            addActionInTryCatch(call, lines, res, baseUrlOfSut)
        } else {
            addActionLines(call, lines, res, baseUrlOfSut)
        }
    }

    private fun addRestCallLines(call: RestCallAction,
                                 lines: Lines,
                                 res: RestCallResult,
                                 baseUrlOfSut: String) {

        //first handle the first line
        val responseVariableName = createUniqueResponseVariableName()

        handleFirstLine(call, lines, res, responseVariableName)

        lines.indent(2)

        when {
            format.isJavaOrKotlin() -> {
                handleHeaders(call, lines)
                handleBody(call, lines)
                handleVerb(baseUrlOfSut, call, lines)
            }
            format.isJavaScript() -> {
                //in SuperAgent, verb must be first
                handleVerb(baseUrlOfSut, call, lines)
                lines.append(getRestAcceptHeader(call, res))
                handleHeaders(call, lines)
                handleBody(call, lines)
            }
            format.isCsharp() -> {
                val hasBody = handleBody(call, lines)
                handleVerb(baseUrlOfSut, call, lines, hasBody)
            }
        }

        handleResponseDirectlyInTheCall(call, res, lines)
        handleLastLine(call, res, lines, responseVariableName)

        handleLocationHeader(call, res, responseVariableName, lines)
        handleResponseAfterTheCall(call, res, responseVariableName, lines)

        //BMR should expectations be here?
        // Having them at the end of a test makes some sense...
        if (shouldCheckExpectations()) {
            handleExpectationSpecificLines(call, lines, res, responseVariableName)
        }
    }

    private fun handleResponseAfterTheCall(call: RestCallAction, res: RestCallResult, responseVariableName: String, lines: Lines) {

        if(format.isJavaOrKotlin() //assertions handled in the call
                || ! needsResponseVariable(call,res)
                || res.failedCall()
                ){
            return
        }

        val code = res.getStatusCode()

        when{
            format.isJavaScript() ->{
                lines.add("expect($responseVariableName.status).toBe($code);")
            }
            else ->{
                LoggingUtil.uniqueWarn(log, "No status assertion supported for format $format")
            }
        }

        if (code == 500) {
            lines.append(" // " + res.getLastStatementWhen500())
        }

        if(config.enableBasicAssertions){
            //TODO
        }

    }

    private fun shouldCheckExpectations() =
            //for now Expectations are only supported on the JVM
            //TODO C# (and maybe JS as well???)
            config.expectationsActive && config.outputFormat.isJavaOrKotlin()


    /**
     * This is done mainly for RestAssured
     */
    private fun handleResponseDirectlyInTheCall(call: RestCallAction, res: RestCallResult, lines: Lines) {
        if (!res.failedCall()) {

            val code = res.getStatusCode()

            when {
                format.isJavaOrKotlin() -> {
                    lines.add(".then()")
                    lines.add(".statusCode($code)")
                }
                else -> throw IllegalStateException("No assertion in calls for format: $format")
                //format.isCsharp() -> lines.add("Assert.Equal($code, (int) response.StatusCode);")
                // This does not work in Superagent. TODO will need after the HTTP call
                //format.isJavaScript() -> lines.add(".expect($code)")
            }

            if (code == 500) {
                lines.append(" // " + res.getLastStatementWhen500())
            }

            if (config.enableBasicAssertions) {
                handleResponseDirectlyInTheCallContents(lines, res)
            }

        } else if (partialOracles.generatesExpectation(call, res)
                && format.isJavaOrKotlin()){
                    //FIXME what is this for???
                    lines.add(".then()")
        }
    }



    private fun handleResponseDirectlyInTheCallContents(lines: Lines, res: RestCallResult) {

        if (format.isJavaScript() || format.isCsharp()) {
            /*
                This is done only for RestAssured... for others, we extract the response object,
                and do assertions on it after the call
             */
            return
        }

        lines.add(".assertThat()")

        if (res.getBodyType() == null) {
            lines.add(".body(isEmptyOrNullString())")
        } else lines.add(
                ".contentType(\"${
                    res.getBodyType()
                            .toString()
                            .split(";").first() //TODO this is somewhat unpleasant. A more elegant solution is needed.
                }\")"
        )

        val bodyString = res.getBody()

        if (res.getBodyType() != null) {
            val type = res.getBodyType()!!
            if (type.isCompatible(MediaType.APPLICATION_JSON_TYPE) || type.toString().toLowerCase().contains("+json")) {
                when (bodyString?.trim()?.first()) {
                    '[' -> {
                        // This would be run if the JSON contains an array of objects.
                        val resContents = Gson().fromJson(res.getBody(), ArrayList::class.java)
                        lines.add(".body(\"size()\", equalTo(${resContents.size}))")
                        //assertions on contents
                        if (resContents.size > 0) {
                            var longArray = false
                            resContents.forEachIndexed { test_index, value ->
                                when {
                                    (value is Map<*, *>) -> handleMapLines(test_index, value, lines)
                                    (value is String) -> longArray = true
                                    else -> {
                                        val printableFieldValue = handleFieldValues(value)
                                        if (printSuitable(printableFieldValue)) {
                                            lines.add(".body(\"\", $printableFieldValue)")
                                        }
                                    }
                                }
                            }
                            if (longArray) {
                                val printableContent = handleFieldValues(resContents)
                                if (printSuitable(printableContent)) {
                                    lines.add(".body(\"\", $printableContent)")
                                }
                            }
                        } else {
                            // the object is empty
                            if (format.isKotlin()) lines.add(".body(\"isEmpty()\", `is`(true))")
                            else lines.add(".body(\"isEmpty()\", is(true))")
                        }
                    }
                    '{' -> {
                        // JSON contains an object
                        val resContents = Gson().fromJson(res.getBody(), Map::class.java)
                        addObjectAssertions(resContents, lines)

                    }
                    else -> {
                        // This branch will be called if the JSON is null (or has a basic type)
                        // Currently, it converts the contents to String.
                        when {
                            res.getTooLargeBody() -> lines.add("/* very large body, which was not handled during the search */")

                            bodyString.isNullOrBlank() -> lines.add(".body(isEmptyOrNullString())")

                            else -> lines.add(
                                    ".body(containsString(\"${
                                        GeneUtils.applyEscapes(
                                                bodyString,
                                                mode = GeneUtils.EscapeMode.BODY,
                                                format = format
                                        )
                                    }\"))"
                            )
                        }
                    }
                }
            } else if (type.isCompatible(MediaType.TEXT_PLAIN_TYPE)) {
                if (bodyString.isNullOrBlank()) {
                    lines.add(".body(isEmptyOrNullString())")
                } else {
                    lines.add(
                            ".body(containsString(\"${
                                GeneUtils.applyEscapes(bodyString, mode = GeneUtils.EscapeMode.TEXT, format = format)
                            }\"))"
                    )
                }
            }
        }
    }

    //TODO: check again for C#, especially when not json
    private fun handleBody(call: RestCallAction, lines: Lines): Boolean {

        var hasBody = false
        val bodyParam = call.parameters.find { p -> p is BodyParam }
        val form = call.getBodyFormData()

        if (bodyParam != null && form != null) {
            throw IllegalStateException("Issue: both Body and FormData present")
        }

        val send = when {
            format.isJavaOrKotlin() -> "body"
            format.isJavaScript() -> "send"
            format.isCsharp() -> ""
            else -> throw IllegalArgumentException("Format not supported $format")
        }

        if (bodyParam != null && bodyParam is BodyParam) {

            when {
                format.isJavaOrKotlin() -> lines.add(".contentType(\"${bodyParam.contentType()}\")")
                format.isJavaScript() -> lines.add(".set('Content-Type','${bodyParam.contentType()}')")
                format.isCsharp() -> lines.add("Client.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue(\"${bodyParam.contentType()}\"));")
            }

            if (bodyParam.isJson()) {

                val json = bodyParam.gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = format)

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
                        hasBody = true
                    } else {
                        lines.add("body = ${bodyLines.first()};")
                        lines.add("httpContent = new StringContent(body, Encoding.UTF8, \"${bodyParam.contentType()}\");")
                        hasBody = true
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
                        lines.add("body = ${bodyLines.first()} +")
                        lines.indented {
                            (1 until bodyLines.lastIndex).forEach { i ->
                                lines.add("${bodyLines[i]} + ")
                            }
                            lines.add("${bodyLines.last()};")
                        }
                        lines.add("httpContent = new StringContent(body, Encoding.UTF8, \"${bodyParam.contentType()}\");")
                    }
                    hasBody = true
                }

            } else if (bodyParam.isTextPlain()) {
                val body =
                        bodyParam.gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.TEXT, targetFormat = format)
                if (body != "\"\"") {
                    if (!format.isCsharp())
                        lines.add(".$send($body)")
                    else {
                        lines.add("body = \"$body\";")
                        lines.add("httpContent = new StringContent(body, Encoding.UTF8, \"${bodyParam.contentType()}\");")
                    }

                    hasBody = true
                } else {
                    if (!format.isCsharp())
                        lines.add(".$send(\"${"""\"\""""}\")")
                    else {
                        lines.add("body = \"${"""\"\""""}\";")
                        lines.add("httpContent = new StringContent(\"${"""\"\""""}\", Encoding.UTF8, \"${bodyParam.contentType()}\");")
                    }
                    hasBody = true
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
                    lines.add("body = \"$body\";")
                    lines.add("httpContent = new StringContent(body, Encoding.UTF8, \"${bodyParam.contentType()}\");")
                }

                hasBody = true
            } else {
                //TODO XML

                LoggingUtil.uniqueWarn(log, "Unrecognized type: " + bodyParam.contentType())
            }
        }

        if (form != null) {
            when {
                format.isJavaOrKotlin() -> lines.add(".contentType(\"application/x-www-form-urlencoded\")")
                format.isJavaScript() -> lines.add(".set('Content-Type','application/x-www-form-urlencoded')")
                format.isCsharp() -> lines.add("Client.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue(\"application/x-www-form-urlencoded\"));")
            }
            if (!format.isCsharp())
                lines.add(".$send(\"$form\")")
            else {
                lines.add("body = \"$form\";")
                lines.add("httpContent = new StringContent(form, Encoding.UTF8, \"application/x-www-form-urlencoded\");")
            }

            hasBody = true
        }
        return hasBody
    }

    private fun handleVerb(baseUrlOfSut: String, call: RestCallAction, lines: Lines, hasBody: Boolean = true) {

        val verb = call.verb.name.toLowerCase()

        if (format.isCsharp()) {
            lines.add("response = await Client.${capitalizeFirstChar(verb)}Async(")
        } else {
            lines.add(".$verb(")
        }

        if (call.locationId != null) {
            if (format.isJavaScript()) {
                lines.append("${TestSuiteWriter.jsImport}")
            }

            //TODO aren't those exactly the same???
            if (format.isCsharp())
                lines.append("resolveLocation(${locationVar(call.locationId!!)}, $baseUrlOfSut + \"${call.resolvedPath()}\")")
            else
                lines.append("resolveLocation(${locationVar(call.locationId!!)}, $baseUrlOfSut + \"${call.resolvedPath()}\")")

        } else {

            if (format.isKotlin()) {
                lines.append("\"\${$baseUrlOfSut}")
            } else {
                lines.append("$baseUrlOfSut + \"")
            }

            if (call.path.numberOfUsableQueryParams(call.parameters) <= 1) {
                val uri = call.path.resolve(call.parameters)
                lines.append("${GeneUtils.applyEscapes(uri, mode = GeneUtils.EscapeMode.URI, format = format)}\"")
            } else {
                //several query parameters. lets have them one per line
                val path = call.path.resolveOnlyPath(call.parameters)
                val elements = call.path.resolveOnlyQuery(call.parameters)

                lines.append("$path?\" + ")

                lines.indented {
                    (0 until elements.lastIndex).forEach { i ->
                        lines.add("\"${GeneUtils.applyEscapes(elements[i], mode = GeneUtils.EscapeMode.SQL, format = format)}&\" + ")
                    }
                    lines.add("\"${GeneUtils.applyEscapes(elements.last(), mode = GeneUtils.EscapeMode.SQL, format = format)}\"")
                }
            }
        }

        if (format.isCsharp()) {
            if (hasBody) {
                if (isVerbWithPossibleBodyPayload(verb))
                    lines.append(", httpContent);")
                else
                    lines.append(");")

            } else {
                if (isVerbWithPossibleBodyPayload(verb))
                    lines.append(", null);")
                else
                    lines.append(");")
            }
            lines.add("responseBody = await response.Content.ReadAsStringAsync();")
        } else
            lines.append(")")
    }

    override fun getAcceptHeader(call: HttpWsAction, res: HttpWsCallResult): String {
        return getRestAcceptHeader(call as RestCallAction, res as RestCallResult)
    }

    private fun getRestAcceptHeader(call: RestCallAction, res: RestCallResult): String {
        /*
         *  Note: using the type in result body is wrong:
         *  if you request a JSON but make an error, you might
         *  get back a text/plain with an explanation
         *
         *  TODO: get the type from the REST call
         */

        val accept = openAcceptHeader()

        var result: String

        if (call.produces.isEmpty() || res.getBodyType() == null) {
            result = "$accept\"*/*\""
        } else {

            val accepted = call.produces.filter { res.getBodyType().toString().contains(it, true) }

            result = if (accepted.size == 1) {
                "$accept\"${accepted.first()}\""
            } else {
                //FIXME: there seems to have been something or a problem
                "$accept\"*/*\""
            }
        }

        result = closeAcceptHeader(result)

        return result
    }


    private fun handleLastLine(call: RestCallAction, res: RestCallResult, lines: Lines, resVarName: String) {

        if(format.isJavaScript()){
            /*
                This is to deal with very weird behavior in SuperAgent that crashes the tests
                for status codes different from 2xx...
                so, here we make it passes as long as a status was present
             */
            lines.add(".ok(res => res.status)")
        }

        lines.appendSemicolon(format)
        lines.deindent(2)
    }

    private fun handleLocationHeader(call: RestCallAction, res: RestCallResult, resVarName: String, lines: Lines) {
        if (call.saveLocation && !res.stopping) {

            if (!res.getHeuristicsForChainedLocation()) {

                //TODO JS and C#
                val extract = "$resVarName.extract().header(\"location\")"

                lines.add("${locationVar(call.path.lastElement())} = $extract")
                lines.appendSemicolon(format)

                /*
                    If there is a "location" header, then it must be either empty or a valid URI.
                    If that is not the case, it would be a bug.
                    But we do not really handle it as "found fault" during the search.
                    Plus the test should not fail, although clearly a bug.
                    But in any case, if invalid URL, following HTTP calls would fail anyway

                    FIXME: should handle it as an extra oracle during the search
                 */

                when {
                    format.isJavaOrKotlin() -> {
                        lines.add("assertTrue(isValidURIorEmpty(${locationVar(call.path.lastElement())}));")
                    }
                    format.isJavaScript() -> {
                        val validCheck = "${TestSuiteWriter.jsImport}.isValidURIorEmpty(${locationVar(call.path.lastElement())})"
                        lines.add("expect($validCheck).toBe(true);")
                    }
                    format.isCsharp() -> {
                        //TODO
                        lines.add("Assert.True(IsValidURIorEmpty(${locationVar(call.path.lastElement())}));")
                    }
                }
            } else {

                val extraTypeInfo = when {
                    format.isKotlin() -> "<Object>"
                    else -> ""
                }
                val baseUri: String = if (call.locationId != null) {
                    locationVar(call.locationId!!)
                } else {
                    call.path.resolveOnlyPath(call.parameters)
                }

                //TODO JS and C#
                val extract = "$resVarName.extract().body().path$extraTypeInfo(\"${res.getResourceIdName()}\").toString()"

                lines.add("${locationVar(call.path.lastElement())} = \"$baseUri/\" + $extract")
                lines.appendSemicolon(format)
            }
        }
    }

    fun addDeclarationsForExpectations(lines: Lines, individual: EvaluatedIndividual<RestIndividual>){
        if(!partialOracles.generatesExpectation(individual)){
            return
        }

        if(! format.isJavaOrKotlin()){
            //TODO will need to see if going to support JS and C# as well
            return
        }

        lines.addEmpty()
        when{
            format.isJava() -> lines.append("ExpectationHandler expectationHandler = expectationHandler()")
            format.isKotlin() -> lines.append("val expectationHandler: ExpectationHandler = expectationHandler()")
        }
        lines.appendSemicolon(format)
    }

    fun handleExpectationSpecificLines(call: RestCallAction, lines: Lines, res: RestCallResult, name: String){
        lines.addEmpty()
        if( partialOracles.generatesExpectation(call, res)){
            partialOracles.addExpectations(call, lines, res, name, format)
        }
    }
}