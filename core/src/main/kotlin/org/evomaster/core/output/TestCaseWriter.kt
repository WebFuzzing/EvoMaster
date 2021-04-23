package org.evomaster.core.output

import com.google.gson.Gson
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.graphql.GraphQlCallResult
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.search.EvaluatedAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.slf4j.LoggerFactory
import javax.ws.rs.core.MediaType


class TestCaseWriter {

    /**
     * In the tests, we might need to generate new variables.
     * We must guarantee that no 2 variables have the same name.
     * Easiest approach is to just use a counter that is incremented
     * at each new generated variable
     */
    private var counter = 0


    //TODO: refactor in constructor, and take out of convertToCompilableTestCode
    private var format: OutputFormat = OutputFormat.JAVA_JUNIT_4
    private lateinit var configuration: EMConfig

    /*
        The following are just for REST
        TODO: refactor, considering we are adding GraphQL and others
     */
    private lateinit var expectationsWriter: ExpectationsWriter
    private lateinit var swagger: OpenAPI
    private lateinit var partialOracles: PartialOracles

    companion object {
        private val log = LoggerFactory.getLogger(TestCaseWriter::class.java)

        /**
         *   Internal flag to mark cases which do not support yet
         */
        const val NOT_COVERED_YET = "NotCoveredYet"
    }

    fun setSwagger(sw: OpenAPI) {
        swagger = sw
    }

    fun setPartialOracles(oracles: PartialOracles) {
        partialOracles = oracles
    }

    fun setupWriter(config: EMConfig, objGenerator: ObjectGenerator) {
        //TODO: refactor remove once changes merged
        configuration = config
        this.format = config.outputFormat
        this.expectationsWriter = ExpectationsWriter()
        expectationsWriter.setFormat(this.format)

        if (config.expectationsActive
                && ::swagger.isInitialized) {
            objGenerator.setSwagger(swagger)
            partialOracles.setGenerator(objGenerator)
            partialOracles.setFormat(format)
            expectationsWriter.setSwagger(swagger)
            expectationsWriter.setPartialOracles(partialOracles)
        }
    }

    fun convertToCompilableTestCode(
            config: EMConfig,
            test: TestCase,
            baseUrlOfSut: String,
            objectGenerator: ObjectGenerator = ObjectGenerator())
            : Lines {

        //TODO: refactor remove once changes merged
        configuration = config
        this.format = config.outputFormat
        this.expectationsWriter = ExpectationsWriter()
        expectationsWriter.setFormat(this.format)

        setupWriter(config, objectGenerator)
        counter = 0

        val lines = Lines()

        if (config.testSuiteSplitType == EMConfig.TestSuiteSplitType.CLUSTER
                && test.test.getClusters().size != 0) {
            clusterComment(lines, test)
        }
        if (format.isJUnit()) {
            lines.add("@Test")
        }
        //TODO: check xUnit instead
        if (format.isCsharp()) {
            lines.add("[Fact]")
        }

        when {
            format.isJava() -> lines.add("public void ${test.name}() throws Exception {")
            format.isKotlin() -> lines.add("fun ${test.name}()  {")
            format.isJavaScript() -> lines.add("test(\"${test.name}\", async () => {")
            format.isCsharp() -> lines.add("public async Task ${test.name}() {")

        }

        lines.indented {

            val ind = test.test

            if (ind.individual is RestIndividual) {
                if (configuration.expectationsActive) {
                    expectationsWriter.addDeclarations(lines, ind as EvaluatedIndividual<RestIndividual>)
                    //TODO: -> also check expectation generation before adding declarations
                }
                if (ind.individual.seeInitializingActions().isNotEmpty()) {
                    SqlWriter.handleDbInitialization(format, ind.individual.seeInitializingActions(), lines)
                }
            } else {
                if (ind.individual is GraphQLIndividual) {
                    //TODO refactor
                    if (ind.individual.dbInitialization.isNotEmpty()) {
                        SqlWriter.handleDbInitialization(format, ind.individual.seeInitializingActions(), lines)
                    }
                }
            }
            if (test.hasChainedLocations()) {
                assert(ind.individual is RestIndividual)
                /*
                    If the "location" header of a HTTP response is used in a following
                    call, we need to save it in a variable.
                    We declare all such variables at the beginning of the test.

                    TODO: rather declare variable first time we access it?
                 */
                lines.addEmpty()


                test.test.evaluatedActions().asSequence()
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
                        }
                    }
            }

            CookieWriter.handleGettingCookies(format, test.test, lines, baseUrlOfSut)
            TokenWriter.handleGettingTokens(format, test.test, lines, baseUrlOfSut)

            //SQL actions are generated in between
            if (test.test.individual is RestIndividual && config.isEnabledSQLInBetween()) {

                test.test.evaluatedResourceActions().forEachIndexed { index, c ->
                    // db
                    if (c.first.isNotEmpty())
                        SqlWriter.handleDbInitialization(format, c.first, lines, test.test.individual.seeDbActions(), groupIndex = index.toString())
                    //actions
                    c.second.forEach { a ->
                        handleEvaluatedAction(a, lines, baseUrlOfSut)
                    }
                }
            } else {if (test.test.individual is RestIndividual) {
                test.test.evaluatedActions().forEach { a ->
                    handleEvaluatedAction(a, lines, baseUrlOfSut)
                }
            }
            }
            if (test.test.individual is GraphQLIndividual) {
                test.test.evaluatedActions().forEach { a ->
                    handleGraphQlCall(a, lines, baseUrlOfSut)
                }
            }
        }
        lines.add("}")

        if (format.isJavaScript()) {
            lines.append(");")
        }
        return lines
    }

    private fun handleEvaluatedAction(a: EvaluatedAction, lines: Lines, baseUrlOfSut: String) {
        when (a.action) {
            is RestCallAction -> handleRestCall(a, lines, baseUrlOfSut)
            else -> throw IllegalStateException("Cannot handle " + a.action.getName())
        }
    }


    private fun locationVar(id: String): String {
        //TODO make sure name is syntactically valid
        //TODO use counters to make them unique
        return "location_${id.trim().replace(" ", "_")}"
    }


    private fun handleRestCall(
            evaluatedAction: EvaluatedAction,
            lines: Lines,
            baseUrlOfSut: String
    ) {
        lines.addEmpty()

        val call = evaluatedAction.action as RestCallAction
        val res = evaluatedAction.result as RestCallResult

        if (res.failedCall()
                || format.isJavaScript() //looks like even 400 throws exception with SuperAgent... :(
        ) {
            addRestCallInTryCatch(call, lines, res, baseUrlOfSut)
        } else {
            addRestCallLines(call, lines, res, baseUrlOfSut)
        }
    }

    private fun handleGraphQlCall(
            evaluatedAction: EvaluatedAction,
            lines: Lines,
            baseUrlOfSut: String
    ) {
        lines.addEmpty()
        val call = evaluatedAction.action as GraphQLAction
        val res = evaluatedAction.result as GraphQlCallResult

        if (res.failedCall()
                || format.isJavaScript() //looks like even 400 throws exception with SuperAgent... :(
        ) {
            addGraphQlCallInTryCatch(call, lines, res, baseUrlOfSut)
        } else {
            addGraphQlCallLines(call, lines, res, baseUrlOfSut)
        }
    }


    private fun addRestCallInTryCatch(call: RestCallAction,
                                      lines: Lines,
                                      res: RestCallResult,
                                      baseUrlOfSut: String) {
        when {
            /*
                TODO do we need to handle differently in JS due to Promises?
             */
            format.isJavaOrKotlin() -> lines.add("try{")
            format.isJavaScript() -> lines.add("try{")
            format.isCsharp() -> lines.add("try{")
        }

        lines.indented {
            addRestCallLines(call, lines, res, baseUrlOfSut)

            if (!res.getTimedout()) {
                /*
                Fail test if exception is not thrown, but not if it was a timeout,
                otherwise the test would become flaky
              */
                if (!format.isJavaScript()) {
                    /*
                        TODO need a way to do it for JS, see
                        https://github.com/facebook/jest/issues/2129
                        what about expect(false).toBe(true)?
                     */
                    lines.add("fail(\"Expected exception\");")
                }
            }
        }

        when {
            format.isJavaOrKotlin() -> lines.add("} catch(Exception e){")
            format.isJavaScript() -> lines.add("} catch(e){")
            format.isCsharp() -> lines.add("} catch(Exception e){")
        }

        res.getErrorMessage()?.let {
            lines.indented {
                lines.add("//$it")
            }
        }
        lines.add("}")
    }


    private fun addGraphQlCallInTryCatch(call: GraphQLAction,
                                         lines: Lines,
                                         res: GraphQlCallResult,
                                         baseUrlOfSut: String) {
        when {
            /*
                TODO do we need to handle differently in JS due to Promises?
             */
            format.isJavaOrKotlin() -> lines.add("try{")
            format.isJavaScript() -> lines.add("try{")
        }

        lines.indented {
            addGraphQlCallLines(call, lines, res, baseUrlOfSut)

            if (!res.getTimedout()) {
                /*
                Fail test if exception is not thrown, but not if it was a timeout,
                otherwise the test would become flaky
              */
                if (!format.isJavaScript()) {
                    /*
                        TODO need a way to do it for JS, see
                        https://github.com/facebook/jest/issues/2129
                        what about expect(false).toBe(true)?
                     */
                    lines.add("fail(\"Expected exception\");")
                }
            }
        }

        when {
            format.isJavaOrKotlin() -> lines.add("} catch(Exception e){")
            format.isJavaScript() -> lines.add("} catch(e){")
        }

        res.getErrorMessage()?.let {
            lines.indented {
                lines.add("//$it")
            }
        }
        lines.add("}")
    }


    private fun createUniqueResponseVariableName(): String {
        val name = "res_$counter"
        counter++
        return name
    }

    private fun addRestCallLines(call: RestCallAction,
                                 lines: Lines,
                                 res: RestCallResult,
                                 baseUrlOfSut: String) {

        //first handle the first line
        val name = createUniqueResponseVariableName()

        handleFirstLine(call, lines, res, name)

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
                lines.append(getAcceptHeader(call, res))
                handleHeaders(call, lines)
                handleBody(call, lines)
            }
            format.isCsharp() -> {
                val hasBody = handleBody(call, lines)
                handleVerb(baseUrlOfSut, call, lines, hasBody)
            }
        }

        handleResponse(call, res, lines)
        handleLastLine(call, res, lines, name)

        //BMR should expectations be here?
        // Having them at the end of a test makes some sense...
        if (configuration.expectationsActive) {
            expectationsWriter.handleExpectationSpecificLines(call, lines, res, name)
        }
    }

    private fun addGraphQlCallLines(call: GraphQLAction,
                                    lines: Lines,
                                    res: GraphQlCallResult,
                                    baseUrlOfSut: String) {

        handleGQLFirstLine(call, lines, res)

        lines.indent(2)

        when {
            format.isJavaOrKotlin() -> {
                handleGQLHeaders(call, lines)
                handleGQLBody(call, lines)
                handleGQLVerb(baseUrlOfSut, call, lines)
            }
            format.isJavaScript() -> {
                //in SuperAgent, verb must be first
                handleGQLVerb(baseUrlOfSut, call, lines)
                lines.append(getAcceptGQLHeader(call, res))
                handleGQLHeaders(call, lines)
                handleGQLBody(call, lines)
            }
        }

        handleGQLResponse(call, res, lines)
        handleGQLLastLine(lines)
    }

    /**
     * When we make a HTTP call, do we need to store the response in a variable for following HTTP calls?
     */
    private fun needsResponseVariable(call: RestCallAction, res: RestCallResult): Boolean {

        return (configuration.expectationsActive
                && partialOracles.generatesExpectation(call, res))
                // || !res.failedCall()
                || (call.saveLocation && !res.stopping)
    }

    private fun handleFirstLine(call: RestCallAction, lines: Lines, res: RestCallResult, resVarName: String) {

        lines.addEmpty()
        if (needsResponseVariable(call, res)) {
            when {
                format.isKotlin() -> lines.append("val $resVarName: ValidatableResponse = ")
                format.isJava() -> lines.append("ValidatableResponse $resVarName = ")
                //TODO JavaScript
            }
        }

        when {
            format.isJavaOrKotlin() -> lines.append("given()")
            format.isJavaScript() -> lines.append("await superagent")
            format.isCsharp() -> lines.append("Client.DefaultRequestHeaders.Clear();\n")
        }
        //TODO: check for C#
        if (!format.isJavaScript()) {
            lines.append(getAcceptHeader(call, res))
        }
    }

    private fun handleGQLFirstLine(call: GraphQLAction, lines: Lines, res: GraphQlCallResult) {

        lines.addEmpty()

        when {
            format.isJavaOrKotlin() -> lines.append("given()")
            format.isJavaScript() -> lines.append("await superagent")
        }

        if (!format.isJavaScript()) {
            lines.append(getAcceptGQLHeader(call, res))
        }
    }


    private fun handleLastLine(call: RestCallAction, res: RestCallResult, lines: Lines, resVarName: String) {

        lines.appendSemicolon(format)
        lines.deindent(2)

        if (call.saveLocation && !res.stopping) {

            if (!res.getHeuristicsForChainedLocation()) {
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

                val extract = "$resVarName.extract().body().path$extraTypeInfo(\"${res.getResourceIdName()}\").toString()"

                lines.add("${locationVar(call.path.lastElement())} = \"$baseUri/\" + $extract")
                lines.appendSemicolon(format)
            }
        }
    }

    private fun handleGQLLastLine(lines: Lines) {

        lines.appendSemicolon(format)
        lines.deindent(2)
        //todo check if correct and check the semicolon
        lines.appendSemicolon(format)
    }
    private fun handleVerb(baseUrlOfSut: String, call: RestCallAction, lines: Lines, hasBody: Boolean = true) {

        var verb = call.verb.name.toLowerCase()

        if (format.isCsharp()) {
            lines.add("response = await Client.${capitalizeFirstChar(verb)}Async(")
        } else {
            lines.add(".$verb(")
        }

        if (call.locationId != null) {
            if (format.isJavaScript()) {
                lines.append("${TestSuiteWriter.jsImport}")
            }

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
                        lines.add("\"${GeneUtils.applyEscapes(elements[i], mode = GeneUtils.EscapeMode.SQL, format = format)}&\" + ") }
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

    private fun capitalizeFirstChar(name: String): String {
        return name[0].toUpperCase() + name.substring(1)
    }

    private fun handleGQLVerb(baseUrlOfSut: String, call: GraphQLAction, lines: Lines) {

        // TODO maybe in future might want to have GET for QUERY types
        val verb = "post"
        lines.add(".$verb(")

        if (format.isKotlin()) {
            lines.append("\"\${$baseUrlOfSut}")
        } else {
            lines.append("$baseUrlOfSut + \"")
        }
        val path = "/graphql"
        lines.append("${GeneUtils.applyEscapes(path, mode = GeneUtils.EscapeMode.NONE, format = format)}\"")
        lines.append(")")
    }

    private fun handleResponse(call: RestCallAction, res: RestCallResult, lines: Lines) {
        if (!res.failedCall()) {

            val code = res.getStatusCode()

            when {
                format.isJavaOrKotlin() -> {
                    lines.add(".then()")
                    lines.add(".statusCode($code)")
                }
                format.isCsharp() -> lines.add("Assert.Equal($code, (int) response.StatusCode);")
                // This does not work in Superagent. TODO will need after the HTTP call
                //format.isJavaScript() -> lines.add(".expect($code)")
            }

            if (code == 500) {
                lines.append(" // " + res.getLastStatementWhen500())
            }

            if (configuration.enableBasicAssertions) {
                handleResponseContents(lines, res)
            }
        } else if (partialOracles.generatesExpectation(call, res)
                && format.isJavaOrKotlin()) lines.add(".then()")
    }

    private fun handleGQLResponse(call: GraphQLAction, res: GraphQlCallResult, lines: Lines) {
        if (!res.failedCall()) {

            val code = res.getStatusCode()

            when {
                format.isJavaOrKotlin() -> {
                    lines.add(".then()")
                    lines.add(".statusCode($code)")
                }
                // This does not work in Superagent. TODO will need after the HTTP call
                //format.isJavaScript() -> lines.add(".expect($code)")
            }

            var commented = false
            if (code == 500) {
                commented = true
                lines.append(" // " + res.getLastStatementWhen500())
            }

            //TODO Man: shall we add lastStatement with errors here?
            if (res.getLastStatementWhenGQLErrors()!=null){
                lines.append("${if (!commented) "//" else ","} errors:${res.getLastStatementWhenGQLErrors()}")
            }

            if (configuration.enableBasicAssertions) {
                handleGQLResponseContents(lines, res)
            }
        }
    }

    private fun handleAdditionalFieldValues(stringKey: String, resContentsItem: Any?): List<Pair<String, String>>?{
        resContentsItem?: return null
        val list = mutableListOf<Pair<String, String>>()
        when (resContentsItem::class) {
            ArrayList::class -> {
                list.add("$stringKey.size()" to "equalTo(${(resContentsItem as ArrayList<*>).size})")
                resContentsItem.forEachIndexed { index, v ->
                    if (v is Map<*, *>){
                        val flatContent = flattenForAssert(mutableListOf<String>(), v)
                        flatContent.keys
                            .filter { !it.contains("timestamp")  && !it.contains("self")}
                            .forEach {key->
                                val fstringKey = key.joinToString(prefix = "\'", postfix = "\'", separator = "\'.\'")
                                val factualValue = flatContent[key]

                                if (factualValue != null){
                                    val key = "$stringKey.get($index).$fstringKey"
                                    list.add(key to handleFieldValues(factualValue))
                                    handleAdditionalFieldValues(key, factualValue)?.let { list.addAll(it) }
                                }
                        }
                    }
                }
            }
        }

        return list
    }

    private fun handleFieldValues(resContentsItem: Any?): String {
        if (resContentsItem == null) {
            return "nullValue()"
        } else {
            when (resContentsItem::class) {
                Double::class -> return "numberMatches(${resContentsItem as Double})"
                String::class -> return "containsString(\"${
                    GeneUtils.applyEscapes(
                        resContentsItem as String,
                        mode = GeneUtils.EscapeMode.ASSERTION,
                        format = format
                    )
                }\")"
                Map::class -> return NOT_COVERED_YET
                ArrayList::class -> if ((resContentsItem as ArrayList<*>).all { it is String } && resContentsItem.isNotEmpty()) {
                    return "hasItems(${
                        (resContentsItem as ArrayList<String>).joinToString {
                            "\"${
                                GeneUtils.applyEscapes(
                                    it,
                                    mode = GeneUtils.EscapeMode.ASSERTION,
                                    format = format
                                )
                            }\""
                        }
                    })"
                } else {
                    return NOT_COVERED_YET
                }
                else -> return NOT_COVERED_YET
            }
        }
        /* BMR: the code above is due to a somewhat unfortunate problem:
        - Gson parses all numbers as Double
        - Hamcrest has a hard time comparing double to int
        The solution is to use an additional content matcher that can be found in NumberMatcher. This can also
        be used as a template for adding more matchers, should such a step be needed.
        * */
    }

    private fun handleMapLines(index: Int, map: Map<*, *>, lines: Lines) {
        map.keys.forEach {
            val printableTh = handleFieldValues(map[it])
            if (printableTh != "null"
                && printableTh != NOT_COVERED_YET
                && !printableTh.contains("logged")
            ) {
                lines.add(".body(\"\'$it\'\", hasItem($printableTh))")
            }
        }
    }

    private fun handleResponseContents(lines: Lines, res: RestCallResult) {

        if (format.isJavaScript()) {
            //TODO
            return
        }

        if (format.isCsharp()) {
            //TODO
            return
        }


        lines.add(".assertThat()")

        if (res.getBodyType() == null) {
            lines.add(".contentType(\"\")")
            if (res.getBody().isNullOrBlank() && res.getStatusCode() != 400) lines.add(".body(isEmptyOrNullString())")

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

    /**
     * response are handled with 'data'
     */
    private fun handleGQLResponseContents(lines: Lines, res: GraphQlCallResult) {

        if (format.isJavaScript()) {
            //TODO
            return
        }

        lines.add(".assertThat()")

        if (res.getBodyType() == null) {
            lines.add(".contentType(\"\")")
            if (res.getBody().isNullOrBlank() && res.getStatusCode() != 400) lines.add(".body(isEmptyOrNullString())")

        } else lines.add(".contentType(\"${res.getBodyType()
                .toString()
                .split(";").first() //TODO this is somewhat unpleasant. A more elegant solution is needed.
        }\")")


        if (res.getBodyType() != null) {
            val bodyString = res.getBody()

            val type = res.getBodyType()!!
            if (type.isCompatible(MediaType.APPLICATION_JSON_TYPE) || type.toString().toLowerCase().contains("+json"))
            {
                when (bodyString?.trim()?.first()) {
                    //TODO, Man: need a check with Asma or Anrea, it seems never be true in GraphQL, shall we delete this option?
                    '[' -> {
                        // This would be run if the JSON contains an array of objects.
                        val resContents = Gson().fromJson(bodyString, ArrayList::class.java)
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
                        val resContents = Gson().fromJson(bodyString, Map::class.java)
                        addObjectAssertions(resContents, lines)

                    }
                    else -> {
                        // This branch will be called if the JSON is null (or has a basic type)
                        // Currently, it converts the contents to String.
                        when {
                            res.getTooLargeBody() -> lines.add("/* very large body, which was not handled during the search */")

                            bodyString.isNullOrBlank() -> lines.add(".body(isEmptyOrNullString())")

                            else -> lines.add(".body(containsString(\"${
                            GeneUtils.applyEscapes(bodyString, mode = GeneUtils.EscapeMode.BODY, format = format)
                            }\"))")
                        }
                    }
                }
            }
        }
    }

    private fun addObjectAssertions(resContents: Map<*, *>, lines: Lines) {
        if (resContents.isEmpty()) {
            if (format.isKotlin()) lines.add(".body(\"isEmpty()\", `is`(true))")
            else lines.add(".body(\"isEmpty()\", is(true))")
        }

        val flatContent = flattenForAssert(mutableListOf<String>(), resContents)
        // Removed size checks for objects.
        flatContent.keys
                .filter { !it.contains("timestamp") } //needed since timestamps will change between runs
                .filter { !it.contains("self") } //TODO: temporary hack. Needed since ports might change between runs.
                .forEach {
                    val stringKey = it.joinToString(prefix = "\'", postfix = "\'", separator = "\'.\'")
                    val actualValue = flatContent[it]
                    if (actualValue != null) {
                        val printableFieldValue = handleFieldValues(actualValue)
                        if (printSuitable(printableFieldValue)) {
                            /*
                                There are some fields like "id" which are often non-deterministic,
                                which unfortunately would lead to flaky tests
                             */
                            if (stringKey != "\'id\'") lines.add(".body(\"${stringKey}\", ${printableFieldValue})")
                        }
                        //handle additional properties for array
                        handleAdditionalFieldValues(stringKey, actualValue)?.forEach {
                            if (printSuitable(it.second) && it.first != "\'id\'")
                                lines.add(".body(\"${it.first}\", ${it.second})")
                        }
                    }
                }


        /* TODO: BMR - We want to avoid time-based fields (timestamps and the like) as they could lead to flaky tests.
        * Even relatively minor timing changes (one second either way) could cause tests to fail
        * as a result, we are now avoiding generating assertions for fields explicitly labeled as "timestamp"
        * Note that this is a temporary (and somewhat hacky) solution.
        * A more elegant and permanent solution could be handled via the flaky test handling (when that will be ready).
        *
        * NOTE: if we have chained locations, then the "id" should be taken from the chained id rather than the test case?
        */
    }

    private fun handleBody(call: RestCallAction, lines: Lines): Boolean {
        return handleBody(call, lines, true)
    }

    private fun handleGQLBody(call: GraphQLAction, lines: Lines) {
        handleGQLBody(call, lines, true)
    }

    //TODO: check again for C#, especially when not json
    private fun handleBody(call: RestCallAction, lines: Lines, readable: Boolean): Boolean {

        var hasBody: Boolean = false
        val bodyParam = call.parameters.find { p -> p is BodyParam }
        val form = call.getBodyFormData()
        var bodyLines: List<String> = emptyList()


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
                bodyLines = body.split("\n").map { s ->
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

    private fun handleGQLBody(call: GraphQLAction, lines: Lines, readable: Boolean) {

        val inputGenes = call.parameters.filterIsInstance<GQInputParam>().map { it.gene }

        val returnGene = call.parameters.find { p -> p is GQReturnParam }?.gene

        val send = when {
            format.isJavaOrKotlin() -> "body"
            format.isJavaScript() -> "send"
            else -> throw IllegalArgumentException("Format not supported $format")
        }

        when {
            format.isJavaOrKotlin() -> lines.add(".contentType(\"application/json\")")
            format.isJavaScript() -> lines.add(".set('Content-Type','application/json')")

        }

        val body = if (call.methodType.toString() == "QUERY") {
            if (inputGenes.isNotEmpty()) {

                val printableInputGene: MutableList<String> = getPrintableInputGene(inputGenes)

                var printableInputGenes = getPrintableInputGenes(printableInputGene)

                if (returnGene == null) {
                    OutputFormatter.JSON_FORMATTER.getFormatted("{\"query\": \"{ ${call.methodName}($printableInputGenes)} \",\"variables\":null}")

                } else {

                    var query = getQuery(returnGene, call)
                    OutputFormatter.JSON_FORMATTER.getFormatted("{\"query\": \"{ ${call.methodName}($printableInputGenes)$query} \",\"variables\":null}")
                }

            } else {

                if (returnGene == null) {

                    OutputFormatter.JSON_FORMATTER.getFormatted("{\"query\" : \"{ ${call.methodName}   }\",\"variables\":null} ")
                } else {

                    var query = getQuery(returnGene, call)
                    OutputFormatter.JSON_FORMATTER.getFormatted("{\"query\" : \" {${call.methodName}  $query  }    \",\"variables\":null} ")

                }
            }

        } else if (call.methodType.toString() == "MUTATION") {
            val printableInputGene: MutableList<String> = getPrintableInputGene(inputGenes)

            var printableInputGenes = getPrintableInputGenes(printableInputGene)

            if (returnGene == null) {//primitive type means without a return gene
                OutputFormatter.JSON_FORMATTER.getFormatted("{\"query\": \" mutation{ ${call.methodName}($printableInputGenes)} \",\"variables\":null}")

            } else {
                var mutation = getMutation(returnGene, call)

                OutputFormatter.JSON_FORMATTER.getFormatted("{ \"query\" : \"mutation{${call.methodName}  ($printableInputGenes)    $mutation    } \",\"variables\":null} ")
            }
        } else {
            LoggingUtil.uniqueWarn(TestCaseWriter.log, " method type not supported yet : ${call.methodType}").toString()
        }


        //needed as JSON uses ""
        val bodyLines = body.split("\n").map { s ->
            "\" " + GeneUtils.applyEscapes(s.trim(), mode = GeneUtils.EscapeMode.BODY, format = format) + " \""
        }

        if (bodyLines.size == 1) {
            lines.add(".$send(${bodyLines.first()})")
        } else {
            lines.add(".$send(${bodyLines.first()} + ")
            lines.indented {
                (1 until bodyLines.lastIndex).forEach { i ->
                    lines.add("${bodyLines[i]} + ")
                }
                lines.add("${bodyLines.last()})")
            }
        }


    }







    private fun handleHeaders(call: RestCallAction, lines: Lines) {

        val prechosenAuthHeaders = call.auth.headers.map { it.name }

        val set = when {
            format.isJavaOrKotlin() -> "header"
            format.isJavaScript() -> "set"
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
            }
        }

        //TODO make sure header was not already set
        val tokenLogin = call.auth.jsonTokenPostLogin
        if(tokenLogin != null){
            lines.add(".$set(\"Authorization\", ${TokenWriter.tokenName(tokenLogin)}) // ${call.auth.name}")
        }
    }

    private fun handleGQLHeaders(call: GraphQLAction, lines: Lines) {

        val prechosenAuthHeaders = call.auth.headers.map { it.name }

        val set = when {
            format.isJavaOrKotlin() -> "header"
            format.isJavaScript() -> "set"
            else -> throw IllegalArgumentException("Not supported format: $format")
        }

        call.auth.headers.forEach {
            lines.add(".$set(\"${it.name}\", \"${it.value}\") // ${call.auth.name}")
        }

        call.parameters.filterIsInstance<HeaderParam>()
                .filter { !prechosenAuthHeaders.contains(it.name) }
                .forEach {
                    lines.add(".$set(\"${it.name}\", ${it.gene.getValueAsPrintableString(targetFormat = format)})")
                }

        val cookieLogin = call.auth.cookieLogin
        if (cookieLogin != null) {
            when {
                format.isJavaOrKotlin() -> lines.add(".cookies(${CookieWriter.cookiesName(cookieLogin)})")
                format.isJavaScript() -> lines.add(".set('Cookies', ${CookieWriter.cookiesName(cookieLogin)})")
            }
        }
    }

    private fun getAcceptHeader(call: RestCallAction, res: RestCallResult): String {
        /*
         *  Note: using the type in result body is wrong:
         *  if you request a JSON but make an error, you might
         *  get back a text/plain with an explanation
         *
         *  TODO: get the type from the REST call
         */

        val accept = when {
            format.isJavaOrKotlin() -> ".accept("
            format.isJavaScript() -> ".set('Accept', "
            format.isCsharp() -> "Client.DefaultRequestHeaders.Add(\"Accept\", "
            else -> throw IllegalArgumentException("Invalid format: $format")
        }

        var result: String

        if (call.produces.isEmpty() || res.getBodyType() == null) {
            result = "$accept\"*/*\")"
        }

        val accepted = call.produces.filter { res.getBodyType().toString().contains(it, true) }

        result = if (accepted.size == 1) {
            "$accept\"${accepted.first()}\")"
        } else {
            //FIXME: there seems to have been something or a problem
            "$accept\"*/*\")"
        }
        if (format.isCsharp()) result = "$result;"

        return result
    }

    private fun getAcceptGQLHeader(call: GraphQLAction, res: GraphQlCallResult): String {

        val accept = when {
            format.isJavaOrKotlin() -> ".accept("
            format.isJavaScript() -> ".set('Accept', "
            else -> throw IllegalArgumentException("Invalid format: $format")
        }

        /**
         * GQL services typically respond using JSON
         */
        if (res.getBodyType() == null) {
            return "$accept\"application/json\")"
        } else

            return "$accept\"application/json\")"
    }

    /**
     * The purpose of the [flattenForAssert] method is to prepare an object for assertion generation.
     * Objects in Responses may be somewhat complex in structure. The goal is to make a map that contains all the
     * leaves of the object, along with the path of keys to get to them.
     *
     * For example, .body("page.size", numberMatches(20.0)) -> in the payload, access the page field, the size field,
     * and assert that the value there is 20.
     */
    private fun flattenForAssert(k: MutableList<*>, v: Any): Map<MutableList<*>, Any> {
        val returnMap = mutableMapOf<MutableList<*>, Any>()
        if (v is Map<*, *>) {
            v.forEach { key, value ->
                if (value == null) {
                    return@forEach
                } else {
                    val innerkey = k.plus(key) as MutableList
                    val innerMap = flattenForAssert(innerkey, value)
                    returnMap.putAll(innerMap)
                }
            }
        } else {
            returnMap[k] = v
        }
        return returnMap
    }

    fun clusterComment(lines: Lines, test: TestCase) {
        lines.add("/**")
        if (test.test.clusterAssignments.size > 0) lines.add("* [${test.name}] is a part of several clusters, as defined by the selected clustering options. ")
        for (c in test.test.clusterAssignments) {
            lines.add("* $c")
        }
        lines.add("*/")
    }

    /**
     * Some content may be lead to problems in the resultant test case.
     * Null values, or content that is not yet handled are can lead to un-compilable generated tests.
     * Removing strings that contain "logged" is a stopgap: Some fields mark that particular issues have been logged and will often provide object references and timestamps.
     * Such information can cause failures upon re-run, as object references and timestamps will differ.
     */

    private fun printSuitable(printableContent: String): Boolean {
        return (printableContent != "null"
                && printableContent != NOT_COVERED_YET
                && !printableContent.contains("logged")
                && !printableContent.contains("""\w+:\d{4,5}""".toRegex()))
    }

    private fun isVerbWithPossibleBodyPayload(verb: String): Boolean {

        var verbs = arrayOf("post", "put", "patch")

        if (verbs.contains(verb.toLowerCase()))
            return true;
        return false;
    }

    fun getPrintableInputGenes(printableInputGene: MutableList<String>): String {

        return printableInputGene.joinToString(",").replace("\"", "\\\"")

    }

    fun getPrintableInputGene(inputGenes: List<Gene>): MutableList<String> {
        val printableInputGene = mutableListOf<String>()
        for (gene in inputGenes) {
            if (gene is EnumGene<*>) {
                val i = gene.getValueAsRawString()
                printableInputGene.add("${gene.name} : $i")
            } else {
                val i = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.GQL_INPUT_MODE)
                printableInputGene.add("${gene.name} : $i")
            }
        }
        return printableInputGene
    }

    fun getMutation(returnGene: Gene, a: GraphQLAction): String {
        return returnGene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
    }

    fun getQuery(returnGene: Gene, a: GraphQLAction): String {
        return returnGene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
    }


}