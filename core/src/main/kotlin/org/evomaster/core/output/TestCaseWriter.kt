package org.evomaster.core.output

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.core.database.DbAction
import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.search.EvaluatedAction
import org.evomaster.core.search.gene.DateTimeGene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.gene.SqlForeignKeyGene
import java.text.SimpleDateFormat


class TestCaseWriter {

    private var counter = 0

    fun convertToCompilableTestCode(
            format: OutputFormat,
            test: TestCase,
            baseUrlOfSut: String)
            : Lines {

        counter = 0

        val lines = Lines()

        if (format.isJUnit()) {
            lines.add("@Test")
        }

        when {
            format.isJava() -> lines.add("public void ${test.name}() throws Exception {")
            format.isKotlin() -> lines.add("fun ${test.name}()  {")
        }

        lines.indent()

        if (test.test.individual is RestIndividual) {

            if (!test.test.individual.dbInitialization.isEmpty()) {

                handleDbInitialization(format, test.test.individual.dbInitialization, lines)

            }
        }


        if (test.hasChainedLocations()) {
            lines.addEmpty()


            test.test.evaluatedActions()
                    .map { ea -> ea.action }
                    .filterIsInstance(RestCallAction::class.java)
                    .filter { a -> a.locationId != null }
                    .map { a -> a.locationId }
                    .distinct()
                    .forEach { id ->
                        val name = locationVar(id!!)
                        when {
                            format.isJava() -> lines.add("String $name = \"\";")
                            format.isKotlin() -> lines.add("var $name = \"\"")
                        }
                    }
        }


        test.test.evaluatedActions().forEach { a ->
            when (a.action) {
                is RestCallAction -> handleRestCall(a, lines, baseUrlOfSut)
                else -> throw IllegalStateException("Cannot handle " + a.action.getName())
            }
        }

        lines.deindent()
        lines.add("}")

        return lines
    }

    private fun handleDbInitialization(format: OutputFormat, dbInitialization: MutableList<DbAction>, lines: Lines) {

        dbInitialization.forEachIndexed { index, dbAction ->

            var newInsertIntoLine = ""
            if (index == 0) {
                when {
                    format.isJava() -> newInsertIntoLine += "List<InsertionDto> "
                    format.isKotlin() -> newInsertIntoLine += "val "
                }
                newInsertIntoLine += "insertions = sql()"
            } else {
                newInsertIntoLine += ".and()"
            }
            newInsertIntoLine += ".insertInto(\"${dbAction.table.name}\", ${dbAction.geInsertionId()}L)"


            dbAction.seeGenes().forEach { g ->

                if (g.isPrintable()) {

                    if (g is SqlForeignKeyGene) {
                        val variableName = g.getVariableName()
                        /**
                         * At this point all pk Ids should be valid
                         * (despite they being NULL or not)
                         **/
                        assert(g.hasValidUniqueIdOfPrimaryKey())
                        if (g.isNull()) {
                            newInsertIntoLine += ".d(\"$variableName\", \"NULL\")"
                        } else {
                            val uniqueId = g.uniqueIdOfPrimaryKey //g.uniqueId
                            newInsertIntoLine += ".r(\"$variableName\", ${uniqueId}L)"
                        }
                    } else if (g is DateTimeGene) {
                        // YYYY-MM-DD HH:MM:SS
                        val variableName = g.getVariableName()
                        val dateStr = g.date.getValueAsRawString()
                        val timeStr = GeneUtils.let {
                            "${it.padded(g.time.hour.value,2)}:${it.padded(g.time.minute.value,2)}:${it.padded(g.time.second.value,2)}"
                        }

                        val printableValue = "\\\"$dateStr $timeStr\\\""
                        newInsertIntoLine += ".d(\"$variableName\", \"$printableValue\")"
                    } else {
                        val variableName = g.getVariableName()
                        val printableValue = StringEscapeUtils.escapeJava(g.getValueAsPrintableString())
                        newInsertIntoLine += ".d(\"$variableName\", \"$printableValue\")"
                    }

                }

            }

            if (index == dbInitialization.size - 1) {
                newInsertIntoLine += ".dtos()"
                when {
                    format.isJava() -> newInsertIntoLine += ";"
                    format.isKotlin() -> {
                    }
                }
            }
            if (index == 1) {
                lines.indent()
            }
            lines.add(newInsertIntoLine)
            if (index > 0 && index == dbInitialization.size - 1) {
                lines.deindent()
            }
        }


        var execInsertionsLine = "controller.execInsertionsIntoDatabase(insertions)"
        when {
            format.isJava() -> execInsertionsLine += ";"
            format.isKotlin() -> {
            }
        }
        lines.add(execInsertionsLine)
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
        lines.indent()

        addRestCallLines(call, lines, res, baseUrlOfSut)

        if (!res.getTimedout()) {
            /*
                Fail test if exception is not thrown, but not if it was a timeout,
                otherwise the test would become flaky
              */
            lines.add("fail(\"Expected exception\");")
        }
        lines.deindent()

        lines.add("} catch(Exception e){")
        res.getErrorMessage()?.let {
            lines.indent()
            lines.add("//$it")
            lines.deindent()
        }
        lines.add("}")
    }

    private fun addRestCallLines(call: RestCallAction,
                                 lines: Lines,
                                 res: RestCallResult,
                                 baseUrlOfSut: String) {

        //first handle the first line
        handleFirstLine(call, lines, res)
        lines.indent(2)

        handleHeaders(call, lines)

        handleBody(call, lines)

        handleVerb(baseUrlOfSut, call, lines)

        handleResponse(lines, res)

        //finally, handle the last line(s)
        handleLastLine(call, res, lines)
    }

    private fun handleLastLine(call: RestCallAction, res: RestCallResult, lines: Lines) {

        if (call.saveLocation && !res.stopping) {

            if (!res.getHeuristicsForChainedLocation()) {
                lines.add(".extract().header(\"location\");")
                lines.addEmpty()
                lines.deindent(2)
                lines.add("assertTrue(isValidURIorEmpty(${locationVar(call.path.lastElement())}));")
            } else {

                lines.add(".extract().body().path(\"${res.getResourceIdName()}\").toString();")
                lines.addEmpty()
                lines.deindent(2)

                val baseUri: String = if (call.locationId != null) {
                    locationVar(call.locationId!!)
                } else {
                    call.path.resolveOnlyPath(call.parameters)
                }

                lines.add("${locationVar(call.path.lastElement())} = \"$baseUri/\" + id_$counter;")
                counter++
            }
        } else {
            lines.append(";")
            lines.deindent(2)
        }
    }

    private fun handleFirstLine(call: RestCallAction, lines: Lines, res: RestCallResult) {
        lines.addEmpty()
        if (call.saveLocation && !res.stopping) {

            if (!res.getHeuristicsForChainedLocation()) {
                lines.append("${locationVar(call.path.lastElement())} = ")
            } else {
                lines.append("String id_$counter = ")
            }
        }
        lines.append("given()" + getAcceptHeader())
    }

    private fun handleVerb(baseUrlOfSut: String, call: RestCallAction, lines: Lines) {
        val verb = call.verb.name.toLowerCase()
        lines.add(".$verb(")
        if (call.locationId != null) {
            lines.append("resolveLocation(${locationVar(call.locationId!!)}, $baseUrlOfSut + \"${call.resolvedPath()}\")")

        } else {

            lines.append("$baseUrlOfSut + ")

            if (call.path.numberOfUsableQueryParams(call.parameters) <= 1) {
                val uri = call.path.resolve(call.parameters)
                lines.append("\"$uri\"")
            } else {
                //several query parameters. lets have them one per line
                val path = call.path.resolveOnlyPath(call.parameters)
                val elements = call.path.resolveOnlyQuery(call.parameters)

                lines.append("\"$path?\" + ")

                lines.indent()
                (0..elements.lastIndex - 1).forEach { i -> lines.add("\"${elements[i]}&\" + ") }
                lines.add("\"${elements.last()}\"")
                lines.deindent()
            }
        }
        lines.append(")")
    }

    private fun handleResponse(lines: Lines, res: RestCallResult) {
        if (!res.failedCall()) {
            lines.add(".then()")
            lines.add(".statusCode(${res.getStatusCode()})")

            //TODO check on body
        }
    }

    private fun handleBody(call: RestCallAction, lines: Lines) {
        handleBody(call, lines, true)
    }

    private fun handleBody(call: RestCallAction, lines: Lines, readable: Boolean) {
        call.parameters.find { p -> p is BodyParam }
                ?.let {
                    lines.add(".contentType(\"application/json\")")

                    val body = if (readable) OutputFormatter.JSON_FORMATTER.getFormatted(it.gene.getValueAsPrintableString())
                    else it.gene.getValueAsPrintableString()

                    //needed as JSON uses ""
                    val bodyLines = body.split("\n").map { s ->
                        "\"" + s.trim().replace("\"", "\\\"") + "\""
                    }

                    if (bodyLines.size == 1) {
                        lines.add(".body(${bodyLines.first()})")
                    } else {
                        lines.add(".body(${bodyLines.first()} + ")
                        lines.indent()
                        (1..bodyLines.lastIndex - 1).forEach { i ->
                            lines.add("${bodyLines[i]} + ")
                        }
                        lines.add("${bodyLines.last()})")
                        lines.deindent()
                    }

                }

        val form = call.getBodyFormData()
        if (!form.isBlank()) {
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
                    lines.add(".header(\"${it.name}\", ${it.gene.getValueAsPrintableString()})")
                }
    }

    private fun getAcceptHeader(): String {
        /*
         *  Note: using the type in result body is wrong:
         *  if you request a JSON but make an error, you might
         *  get back a text/plain with an explanation
         *
         *  TODO: get the type from the REST call
         */
        return ".accept(\"*/*\")"
    }

}