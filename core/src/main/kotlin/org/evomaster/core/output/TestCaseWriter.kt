package org.evomaster.core.output

import com.google.gson.Gson
import io.swagger.models.Swagger
import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.CookieLogin
import org.evomaster.core.output.service.ObjectGenerator
import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.UsedObjects
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.search.EvaluatedAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.sql.SqlWrapperGene
import javax.ws.rs.core.MediaType


class TestCaseWriter {

    private var counter = 0
    private var usedObjects = UsedObjects()
    private var previousChained = false
    private var previousId = ""
    private var chained = false

    //TODO: refactor in constructor, and take out of convertToCompilableTestCode
    private var format: OutputFormat = OutputFormat.JAVA_JUNIT_4
    private lateinit var configuration: EMConfig
    private lateinit var expectationsWriter: ExpectationsWriter
    private lateinit var swagger: Swagger

    companion object{
        val NOT_COVERED_YET = "NotCoveredYet"
    }

    fun convertToCompilableTestCode(
            config: EMConfig,
            test: TestCase,
            baseUrlOfSut: String)
            : Lines {

        //TODO: refactor remove once changes merged
        configuration = config
        this.format = config.outputFormat
        this.expectationsWriter = ExpectationsWriter()
        expectationsWriter.setFormat(this.format)

        val objGenerator = ObjectGenerator()
        val partialOracles = PartialOracles()

        if(config.expectationsActive
                && ::swagger.isInitialized){
            objGenerator.setSwagger(swagger)
            partialOracles.setGenerator(objGenerator)
            partialOracles.setFormat(format)
            expectationsWriter.setSwagger(swagger)
            expectationsWriter.setPartialOracles(partialOracles)
        }

        counter = 0

        val lines = Lines()

        if (format.isJUnit()) {
            lines.add("@Test")
        }

        when {
            format.isJava() -> lines.add("public void ${test.name}() throws Exception {")
            format.isKotlin() -> lines.add("fun ${test.name}()  {")
        }

        lines.indented {

            val ind = test.test.individual

            if (ind is RestIndividual) {
                // BMR: test.test should have the used objects attached (if any).
                //if (config.enableCompleteObjects) {
                //    usedObjects = ind.usedObjects.copy()
                //}
                if(configuration.expectationsActive){
                    expectationsWriter.addDeclarations(lines)
                }
                if (ind.dbInitialization.isNotEmpty()) {
                    handleDbInitialization(format, ind.dbInitialization, lines)
                }
            }

            if (test.hasChainedLocations()) {
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
                            }
                        }
            }

            handleGettingCookies(test.test, lines, baseUrlOfSut)

            test.test.evaluatedActions().forEach { a ->
                when (a.action) {
                    is RestCallAction -> handleRestCall(a, lines, baseUrlOfSut)
                    else -> throw IllegalStateException("Cannot handle " + a.action.getName())
                }
            }
        }
        lines.add("}")

        return lines
    }

    private fun cookiesName(info: CookieLogin) : String = "cookies_${info.username}"


    private fun handleGettingCookies(ind: EvaluatedIndividual<*>,
                                     lines: Lines,
                                     baseUrlOfSut: String){

        val cookiesInfo = (ind.individual as RestIndividual).getCookieLoginAuth()

        if(cookiesInfo.isNotEmpty()){
            lines.addEmpty()
        }

        for(k in cookiesInfo){

            when {
                format.isJava() -> lines.add("final Map<String,String> ${cookiesName(k)} = ")
                format.isKotlin() -> lines.add("val ${cookiesName(k)} : Map<String,String> = ")
            }

            lines.append("given()")
            lines.indented {

                if(k.contentType == ContentType.X_WWW_FORM_URLENCODED) {
                    lines.add(".formParam(\"${k.usernameField}\", \"${k.username}\")")
                    lines.add(".formParam(\"${k.passwordField}\", \"${k.password}\")")
                } else {
                    throw IllegalStateException("Currently not supporting yet ${k.contentType} in login")
                }

                lines.add(".post(")
                if(format.isJava()) {
                    lines.append("$baseUrlOfSut + \"")
                } else {
                    lines.append("\"\${$baseUrlOfSut}")
                }
                lines.append("${k.loginEndpointUrl}\")")

                lines.add(".then().extract().cookies()") //TODO check response status and cookie headers?
                appendSemicolon(lines)

                lines.addEmpty()
            }
        }
    }

    private fun appendSemicolon(lines: Lines) {
        if (format.isJava()) {
            lines.append(";")
        }
    }

    private fun handleDbInitialization(format: OutputFormat, dbInitialization: List<DbAction>, lines: Lines) {

        if (dbInitialization.isEmpty() || dbInitialization.none { !it.representExistingData }) {
            return
        }

        dbInitialization
                .filter { !it.representExistingData }
                .forEachIndexed { index, dbAction ->

                    lines.add(when {
                        index == 0 && format.isJava() -> "List<InsertionDto> insertions = sql()"
                        index == 0 && format.isKotlin() -> "val insertions = sql()"
                        else -> ".and()"
                    } + ".insertInto(\"${dbAction.table.name}\", ${dbAction.geInsertionId()}L)")

                    if (index == 0) {
                        lines.indent()
                    }

                    lines.indented {
                        dbAction.seeGenes()
                                .filter { it.isPrintable() }
                                .forEach { g ->
                                    when {
                                        g is SqlWrapperGene && g.getForeignKey() != null -> {
                                            val line = handleFK(g.getForeignKey()!!, dbAction, dbInitialization)
                                            lines.add(line)
                                        }
                                        g is ObjectGene -> {
                                            val variableName = g.getVariableName()
                                            val printableValue = getPrintableValue(g)
                                            lines.add(".d(\"$variableName\", \"'$printableValue'\")")
                                        }
                                        else -> {
                                            val variableName = g.getVariableName()
                                            val printableValue = getPrintableValue(g)
                                            lines.add(".d(\"$variableName\", \"$printableValue\")")
                                        }
                                    }
                                }

                    }
                }

        lines.add(".dtos()")
        appendSemicolon(lines)

        lines.deindent()

        lines.add("controller.execInsertionsIntoDatabase(insertions)")
        appendSemicolon(lines)
    }

    private fun getPrintableValue(g: Gene): String {
        if (g is SqlPrimaryKeyGene) {

            return getPrintableValue(g.gene)

        } else {
            return StringEscapeUtils.escapeJava(g.getValueAsPrintableString(targetFormat = format))
            //TODO this is an atypical treatment of escapes. Should we run all escapes through the same procedure?
            // or is this special enough to be justified?
        }
    }

    private fun handleFK(fkg: SqlForeignKeyGene, action: DbAction, allActions: List<DbAction>): String {


        /*
            TODO: why the code here is not relying on SqlForeignKeyGene#getValueAsPrintableString ???
         */

        val variableName = fkg.getVariableName()
        /**
         * At this point all pk Ids should be valid
         * (despite they being NULL or not)
         **/
        Lazy.assert { fkg.hasValidUniqueIdOfPrimaryKey() }
        if (fkg.isNull()) {
            return ".d(\"$variableName\", \"NULL\")"
        }


        val uniqueIdOfPrimaryKey = fkg.uniqueIdOfPrimaryKey

        /*
            TODO: the code here is not handling multi-column PKs/FKs
         */
        val pkExisting = allActions
                .filter { it.representExistingData }
                .flatMap { it.seeGenes() }
                .filterIsInstance<SqlPrimaryKeyGene>()
                .find { it.uniqueId == uniqueIdOfPrimaryKey}

        /*
           This FK might point to a PK of data already existing in the database.
           In such cases, the PK will not be part of the generated SQL commands, and
           we cannot use a "r()" reference to it.
           We need to put the actual value data in a "d()"
        */

        if(pkExisting != null){
            val pk = getPrintableValue(pkExisting)
            return ".d(\"$variableName\", \"$pk\")"
        }

        /*
            Check if this is a reference to an auto-increment
         */
        val keepAutoGeneratedValue = action.selectedColumns
                .filter { it.name == fkg.name }
                .first().foreignKeyToAutoIncrement

        if (keepAutoGeneratedValue) {
            return ".r(\"$variableName\", ${uniqueIdOfPrimaryKey}L)"
        }


        val pkg = allActions
                .flatMap { it.seeGenes() }
                .filterIsInstance<SqlPrimaryKeyGene>()
                .find { it.uniqueId == uniqueIdOfPrimaryKey}!!

        val pk = getPrintableValue(pkg)
        return ".d(\"$variableName\", \"$pk\")"
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

        if (res.failedCall()) {
            addRestCallInTryCatch(call, lines, res, baseUrlOfSut)
        } else {
            addRestCallLines(call, lines, res, baseUrlOfSut)
        }
    }

    private fun addRestCallInTryCatch(call: RestCallAction,
                                      lines: Lines,
                                      res: RestCallResult,
                                      baseUrlOfSut: String) {

        lines.add("try{")
        lines.indented {
            addRestCallLines(call, lines, res, baseUrlOfSut)

            if (!res.getTimedout()) {
                /*
                Fail test if exception is not thrown, but not if it was a timeout,
                otherwise the test would become flaky
              */
                lines.add("fail(\"Expected exception\");")
            }
        }

        lines.add("} catch(Exception e){")

        res.getErrorMessage()?.let {
            lines.indented {
                lines.add("//$it")
            }
        }
        lines.add("}")
    }

    private fun addRestCallLines(call: RestCallAction,
                                 lines: Lines,
                                 res: RestCallResult,
                                 baseUrlOfSut: String) {

        //first handle the first line
        val name = "call_$counter"

        if(configuration.expectationsActive){
            val header = getAcceptHeader(call, res)
            expectationsWriter.handleGenericFirstLine(call, lines, res, name, header)
        }
        else {
            handleFirstLine(call, lines, res)
        }

        lines.indent(2)

        handleHeaders(call, lines)
        handleBody(call, lines)
        handleVerb(baseUrlOfSut, call, lines)
        handleResponse(lines, res)

        //finally, handle the last line(s)
        if(configuration.expectationsActive){
            handleGenericLastLine(call, res, lines, counter)

            previousChained = res.getHeuristicsForChainedLocation()
            if(previousChained) previousId = "id_$counter"
            counter++
        }
        else {
            handleLastLine(call, res, lines)
        }

        //BMR should expectations be here?
        // Having them at the end of a test makes some sense...
        if(configuration.expectationsActive){
            expectationsWriter.handleExpectationSpecificLines(call, lines, res, name)
            expectationsWriter.handleExpectations(call, lines, res, true, name)
        }
        //TODO: BMR expectations from partial oracles here?



    }

    private fun handleLastLine(call: RestCallAction, res: RestCallResult, lines: Lines) {

        if (call.saveLocation && !res.stopping) {

            if (!res.getHeuristicsForChainedLocation()) {
                lines.add(".extract().header(\"location\");")
                lines.addEmpty()
                lines.deindent(2)
                lines.add("assertTrue(isValidURIorEmpty(${locationVar(call.path.lastElement())}));")
            } else {
                //TODO BMR: this is a less-than-subtle way to try to fix a problem in ScoutAPI
                // The test generated in java causes a fail due to .path<Object>
                val extraTypeInfo = when {
                    format.isKotlin() -> "<Object>"
                    else -> ""
                }
                lines.add(".extract().body().path$extraTypeInfo(\"${res.getResourceIdName()}\").toString();")
                lines.addEmpty()
                lines.deindent(2)

                val baseUri: String = if (call.locationId != null) {
                    locationVar(call.locationId!!)
                } else {
                    call.path.resolveOnlyPath(call.parameters)
                }

                lines.add("${locationVar(call.path.lastElement())} = \"$baseUri/\" + id_$counter;")

                previousChained = res.getHeuristicsForChainedLocation()
                if(previousChained) previousId = "id_$counter"
                counter++
            }
        } else {
            appendSemicolon(lines)
            lines.deindent(2)
        }
    }

    private fun handleFirstLine(call: RestCallAction, lines: Lines, res: RestCallResult) {
        lines.addEmpty()
        if (call.saveLocation && !res.stopping) {

            if (!res.getHeuristicsForChainedLocation()) {
                lines.append("${locationVar(call.path.lastElement())} = ")
            } else {
                if (format.isJava()) {
                    lines.append("String id_$counter = ")
                } else {
                    lines.append("val id_$counter: String = ")
                }
                chained = res.getHeuristicsForChainedLocation()
            }
        }
        lines.append("given()" + getAcceptHeader(call, res))
    }

    private fun handleVerb(baseUrlOfSut: String, call: RestCallAction, lines: Lines) {
        val verb = call.verb.name.toLowerCase()
        lines.add(".$verb(")
        if (call.locationId != null) {
            lines.append("resolveLocation(${locationVar(call.locationId!!)}, $baseUrlOfSut + \"${call.resolvedPath()}\")")

        } else {

            if(format.isJava()) {
                lines.append("$baseUrlOfSut + \"")
            } else {
                lines.append("\"\${$baseUrlOfSut}")
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
                    (0 until elements.lastIndex).forEach { i -> lines.add("\"${GeneUtils.applyEscapes(elements[i], mode = GeneUtils.EscapeMode.SQL, format = format)}&\" + ") }
                    lines.add("\"${GeneUtils.applyEscapes(elements.last(), mode = GeneUtils.EscapeMode.SQL, format = format)}\"")
                }
            }
        }
        lines.append(")")
    }

    private fun handleResponse(lines: Lines, res: RestCallResult) {
        if (!res.failedCall()) {
            lines.add(".then()")

            val code = res.getStatusCode()
            lines.add(".statusCode($code)")
            if(code == 500){
                lines.append(" // " + res.getLastStatementWhen500())
            }


            if (configuration.enableBasicAssertions) {
                handleResponseContents(lines, res)
            }

            //TODO check on body
        }
    }

    private fun handleFieldValues(resContentsItem: Any?): String {
        if (resContentsItem == null) {
            return "nullValue()"
        } else {
            when (resContentsItem::class) {
                Double::class -> return "numberMatches(${resContentsItem as Double})"
                String::class -> return "containsString(\"${GeneUtils.applyEscapes(resContentsItem as String, mode = GeneUtils.EscapeMode.ASSERTION, format = format)}\")"
                Map::class -> return NOT_COVERED_YET
                ArrayList::class -> return NOT_COVERED_YET
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

    private fun handleMapLines(index: Int, map: Map<*,*>, lines: Lines){
        map.keys.forEach{
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
        lines.add(".assertThat()")

        if (res.getBodyType() == null) {
            lines.add(".contentType(\"\")")
            if(res.getBody().isNullOrBlank() && res.getStatusCode()!=400) lines.add(".body(isEmptyOrNullString())")

        }
        else lines.add(".contentType(\"${res.getBodyType()
                .toString()
                .split(";").first() //TODO this is somewhat unpleasant. A more elegant solution is needed.
        }\")")

        val bodyString = res.getBody()

        if (res.getBodyType() != null) {
            val type = res.getBodyType()!!
            if (type.isCompatible(MediaType.APPLICATION_JSON_TYPE) || type.toString().toLowerCase().contains("+json")) {
                when (bodyString?.first()) {
                    '[' -> {
                        // This would be run if the JSON contains an array of objects.
                        val resContents = Gson().fromJson(res.getBody(), ArrayList::class.java)
                        lines.add(".body(\"size()\", equalTo(${resContents.size}))")
                        //assertions on contents
                        if(resContents.size > 0){
                            resContents.forEachIndexed { test_index, value ->
                                if (value is Map<*, *>){
                                    handleMapLines(test_index, value, lines)
                                }
                                else {
                                    val printableTh = handleFieldValues(value)
                                    if (printableTh != "null"
                                            && printableTh != NOT_COVERED_YET
                                            && !printableTh.contains("logged")
                                            && !printableTh.contains("""\w+:\d{4,5}""".toRegex())
                                    ) {
                                        lines.add(".body(\"get($test_index)\", $printableTh)")
                                    }
                                }
                            }
                        }
                        else{
                            // the object is empty
                            if(format.isKotlin())  lines.add(".body(\"isEmpty()\", `is`(true))")
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

                            else -> lines.add(".body(containsString(\"${
                                        GeneUtils.applyEscapes(bodyString, mode = GeneUtils.EscapeMode.BODY, format = format)
                                    }\"))")
                        }
                    }
                }
            }
            else if (type.isCompatible(MediaType.TEXT_PLAIN_TYPE)){
                if(bodyString.isNullOrBlank()){
                    lines.add(".body(isEmptyOrNullString())")
                }else {
                    lines.add(".body(containsString(\"${
                    GeneUtils.applyEscapes(bodyString, mode = GeneUtils.EscapeMode.TEXT, format = format)
                    }\"))")
                }
            }
        }
    }

    private fun addObjectAssertions(resContents: Map<*,*>, lines: Lines){
        if (resContents.isEmpty()){
            // If this executes, the result contains an empty collection.
            //lines.add(".body(\"size()\", numberMatches(0))")
            if(format.isKotlin())  lines.add(".body(\"isEmpty()\", `is`(true))")
            else lines.add(".body(\"isEmpty()\", is(true))")
        }

        val flatContent = flattenForAssert(mutableListOf<String>(), resContents)
        // Removed size checks for objects.
        //lines.add(".body(\"size()\", numberMatches(${resContents.size}))")
        flatContent.keys
                .filter{ !it.contains("timestamp")} //needed since timestamps will change between runs
                .filter{ !it.contains("self")} //TODO: temporary hack. Needed since ports might change between runs.
                .forEach {
                    val stringKey = it.joinToString(prefix = "\'", postfix = "\'", separator = "\'.\'")
                    val actualValue = flatContent[it]
                    if(actualValue!=null){
                        val printableTh = handleFieldValues(actualValue)
                        if (printableTh != "null"
                                && printableTh != NOT_COVERED_YET
                                && !printableTh.contains("logged")
                                && !printableTh.contains("""\w+:\d{4,5}""".toRegex())
                        ) {
                            //lines.add(".body(\"\'${it}\'\", ${printableTh})")
                            if(stringKey != "\'id\'") lines.add(".body(\"${stringKey}\", ${printableTh})")
                            else{
                                if(!chained && previousChained) lines.add(".body(\"${stringKey}\", numberMatches($previousId))")
                            }
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

    private fun handleBody(call: RestCallAction, lines: Lines) {
        handleBody(call, lines, true)
    }

    private fun handleBody(call: RestCallAction, lines: Lines, readable: Boolean) {

        val bodyParam = call.parameters.find { p -> p is BodyParam }
        val form = call.getBodyFormData()

        if (bodyParam != null && form != null) {
            throw IllegalStateException("Issue: both Body and FormData present")
        }

        if (bodyParam != null && bodyParam is BodyParam) {

            lines.add(".contentType(\"${bodyParam.contentType()}\")")

            if (bodyParam.isJson()) {

                val body = if (readable) {
                    OutputFormatter.JSON_FORMATTER.getFormatted(bodyParam.gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = format))
                } else {
                    bodyParam.gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = format)
                }

                //needed as JSON uses ""
                val bodyLines = body.split("\n").map { s ->
                     "\" " + GeneUtils.applyEscapes(s.trim(), mode = GeneUtils.EscapeMode.BODY, format = format) + " \""
                }

                if (bodyLines.size == 1) {
                    lines.add(".body(${bodyLines.first()})")
                } else {
                    lines.add(".body(${bodyLines.first()} + ")
                    lines.indented {
                        (1 until bodyLines.lastIndex).forEach { i ->
                            lines.add("${bodyLines[i]} + ")
                        }
                        lines.add("${bodyLines.last()})")
                    }
                }

            } else if (bodyParam.isTextPlain()) {
                val body = bodyParam.gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.TEXT, targetFormat = format)
                if (body != "\"\"") {
                    lines.add(".body($body)")
                }
                else {
                    lines.add(".body(\"${"""\"\""""}\")")
                }

                //BMR: this is needed because, if the string is empty, it causes a 400 (bad request) code on the test end.
                // inserting \"\" should prevent that problem
                // TODO: get some tests done of this
            } else {
                throw IllegalStateException("Unrecognized type: " + bodyParam.contentType())
            }
        }

        if (form != null) {
            lines.add(".contentType(\"application/x-www-form-urlencoded\")")
            lines.add(".body(\"$form\")")
        }
    }

    private fun handleHeaders(call: RestCallAction, lines: Lines) {

        val prechosenAuthHeaders = call.auth.headers.map { it.name }

        call.auth.headers.forEach {
            lines.add(".header(\"${it.name}\", \"${it.value}\") // ${call.auth.name}")
        }

        call.parameters.filterIsInstance<HeaderParam>()
                .filter { !prechosenAuthHeaders.contains(it.name) }
                .forEach {
                    lines.add(".header(\"${it.name}\", ${it.gene.getValueAsPrintableString(targetFormat = format)})")
                }

        val cookieLogin = call.auth.cookieLogin
        if(cookieLogin != null){
            lines.add(".cookies(${cookiesName(cookieLogin)})")
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

        if (call.produces.isEmpty() || res.getBodyType() == null){
            return ".accept(\"*/*\")"
        }

        val accepted = call.produces.filter { res.getBodyType().toString().contains(it, true) }

        if (accepted.size == 1)
            return ".accept(\"${accepted.first()}\")"
        else
            //FIXME: there seems to have been something or a problem
            return ".accept(\"*/*\")"
    }

    /**
     * The purpose of the [flattenForAssert] method is to prepare an object for assertion generation.
     * Objects in Responses may be somewhat complex in structure. The goal is to make a map that contains all the
     * leaves of the object, along with the path of keys to get to them.
     *
     * For example, .body("page.size", numberMatches(20.0)) -> in the payload, access the page field, the size field,
     * and assert that the value there is 20.
     */
    private fun flattenForAssert(k: MutableList<*>, v: Any): Map<MutableList<*>, Any>{
        val returnMap = mutableMapOf<MutableList<*>, Any>()
        if (v is Map<*,*>){
            v.forEach { key, value ->
                if (value == null){
                    return@forEach
                }
                else{
                    val innerkey = k.plus(key) as MutableList
                    val innerMap = flattenForAssert(innerkey, value)
                    returnMap.putAll(innerMap)
                }
            }
        }
        else{
            returnMap[k] = v
        }
        return returnMap
    }

    fun setSwagger(sw: Swagger){
        swagger = sw
    }

    fun handleGenericLastLine(call: RestCallAction, res: RestCallResult, lines: Lines, counter: Int){
        if(format.isJava()) {lines.append(";")}
        lines.deindent(2)

        if (call.saveLocation && !res.stopping){

            var extract: String = ""
            val baseUri: String = if (call.locationId != null) {
                locationVar(call.locationId!!)
            } else {
                call.path.resolveOnlyPath(call.parameters)
            }
            if (!res.getHeuristicsForChainedLocation()){
                extract = "call_$counter.extract().header(\"location\")"
                lines.add("${locationVar(call.path.lastElement())} = $extract")
                appendSemicolon(lines)
            }
            else {
                val extraTypeInfo = when {
                    format.isKotlin() -> "<Object>"
                    else -> ""
                }
                extract = "call_$counter.extract().body().path$extraTypeInfo(\"${res.getResourceIdName()}\").toString()"
                when {
                    format.isJava() -> lines.add("String id_$counter = $extract")
                    format.isKotlin() -> lines.add("val id_$counter: String = $extract")
                }
                lines.add("${locationVar(call.path.lastElement())} = \"$baseUri/\" + id_$counter")
                appendSemicolon(lines)
            }
        }
    }
}