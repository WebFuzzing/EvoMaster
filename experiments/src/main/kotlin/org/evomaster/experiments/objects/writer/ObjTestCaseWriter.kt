package org.evomaster.experiments.objects.writer

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.core.database.DbAction
import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.EvaluatedAction
import org.evomaster.core.search.gene.*
import org.evomaster.core.output.*
import org.evomaster.experiments.objects.ObjIndividual
import org.evomaster.experiments.objects.ObjRestCallAction
import org.evomaster.experiments.objects.param.BodyParam
import org.evomaster.experiments.objects.param.HeaderParam


class ObjTestCaseWriter {

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

        /*
        if (test.test.individual is ObjIndividual) {
            if (!test.test.individual.dbInitialization.isEmpty()) {

                handleDbInitialization(format, test.test.individual.dbInitialization, lines)

            }
        }*/


        if (test.hasChainedLocations()) {
            lines.addEmpty()


            test.test.evaluatedActions()
                    .map { it.action }
                    .filterIsInstance(ObjRestCallAction::class.java)
                    .filter { it.locationId != null }
                    .map { it.locationId }
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
                is ObjRestCallAction -> handleRestCall(a, lines, baseUrlOfSut)
                else -> throw IllegalStateException("Cannot handle " + a.action.getName())
            }
        }

        lines.deindent()
        lines.add("}")

        //TODO: Remove this ASAP :D it's just meant to be a test of the usedObj and potential adding to TC

        println("Individual: ${test.test.individual} uses => ${(test.test.individual as ObjIndividual).usedObject.displayInline()} ")
        val goodgets = test.test.results.filter {result ->
            (result as RestCallResult).getStatusCode() == 200
        }

        val goodputs = test.test.results.filter { result ->
            (result as RestCallResult).getStatusCode() == 201
        }.forEach{ res ->
            val goodput = (res as RestCallResult).getBody()
        }

        goodgets.forEach { res ->
            val te = (res as RestCallResult).getBody()
        }


        return lines
    }

    /*
    fun handleDbInitialization(format: OutputFormat, dbInitialization: MutableList<DbAction>, lines: Lines) {


        dbInitialization.forEachIndexed { index, dbAction ->

            lines.add(when {
                index == 0 && format.isJava() -> "List<InsertionDto> insertions = sql()"
                index == 0 && format.isKotlin() -> "val insertions = sql()"
                else -> ".and()"
            } + ".insertInto(\"${dbAction.table.name}\", ${dbAction.geInsertionId()}L)")

            if (index == 0) {
                lines.indent()
            }

            lines.indent()
            dbAction.seeGenes().forEach { g ->

                if (g.isPrintable()) {

                    when {
                        g is SqlForeignKeyGene -> {
                            val line = handleFK(g, dbAction)
                            lines.add(line)
                        }
                        (g is SqlPrimaryKeyGene) && (g.gene is SqlForeignKeyGene) -> {
                            /*
                                TODO: this will need to be refactored when Gene system
                                will have "previousGenes"-based methods on all genes
                             */
                            val line = handleFK(g.gene, dbAction)
                            lines.add(line)
                        }
                        else -> {
                            val variableName = g.getVariableName()
                            val printableValue = getPrintableValue(g)
                            lines.add(".d(\"$variableName\", \"$printableValue\")")
                        }
                    }
                }
            }
            lines.deindent()

            if (index == dbInitialization.size - 1) {
                lines.add(".dtos()" +
                        when {
                            format.isJava() -> ";"
                            format.isKotlin() -> ""
                            else -> ""

                        })
            }

        }
        lines.deindent()

        var execInsertionsLine = "controller.execInsertionsIntoDatabase(insertions)"
        when {
            format.isJava() -> execInsertionsLine += ";"
            format.isKotlin() -> {
            }
        }
        lines.add(execInsertionsLine)
    }

    */

    private fun getPrintableValue(g: Gene): String {
        if (g is SqlPrimaryKeyGene) {

            return getPrintableValue(g.gene)

        } else {
            return StringEscapeUtils.escapeJava(g.getValueAsPrintableString(targetFormat = null))
        }
    }

    private fun handleFK(g: SqlForeignKeyGene, action: DbAction): String {


        /*
            TODO: why the code here is not relying on SqlForeignKeyGene#getValueAsPrintableString ???
         */

        val variableName = g.getVariableName()
        /**
         * At this point all pk Ids should be valid
         * (despite they being NULL or not)
         **/
        assert(g.hasValidUniqueIdOfPrimaryKey())
        return if (g.isNull()) {
            ".d(\"$variableName\", \"NULL\")"
        } else {
            val keepAutoGeneratedValue = action.selectedColumns.filter { it.name == g.name }.first().foreignKeyToAutoIncrement
            val uniqueId = g.uniqueIdOfPrimaryKey //g.uniqueId
            if (keepAutoGeneratedValue) {
                ".r(\"$variableName\", ${uniqueId}L, true)"
            } else {
                ".r(\"$variableName\", ${uniqueId}L)"
            }
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

        val call = evaluatedAction.action as ObjRestCallAction
        val res = evaluatedAction.result as RestCallResult

        if (res.failedCall()) {
            addRestCallInTryCatch(call, lines, res, baseUrlOfSut)
        } else {
            addRestCallLines(call, lines, res, baseUrlOfSut)
        }
    }

    private fun addRestCallInTryCatch(call: ObjRestCallAction,
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

    private fun addRestCallLines(call: ObjRestCallAction,
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

    private fun handleLastLine(call: ObjRestCallAction, res: RestCallResult, lines: Lines) {

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

    private fun handleFirstLine(call: ObjRestCallAction, lines: Lines, res: RestCallResult) {
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

    private fun handleVerb(baseUrlOfSut: String, call: ObjRestCallAction, lines: Lines) {
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
                (0 until elements.lastIndex).forEach { i -> lines.add("\"${elements[i]}&\" + ") }
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
            //assertReturnSimple(lines, res)
            //TODO check on body
        }
    }

    private fun handleBody(call: ObjRestCallAction, lines: Lines) {
        handleBody(call, lines, true)
    }

    private fun handleBody(call: ObjRestCallAction, lines: Lines, readable: Boolean) {
        call.parameters.find { p -> p is BodyParam }
                ?.let {
                    lines.add(".contentType(\"application/json\")")

                    val body = if (readable) OutputFormatter.JSON_FORMATTER.getFormatted(it.gene.getValueAsPrintableString(targetFormat = null))
                    else it.gene.getValueAsPrintableString(targetFormat = null)

                    //needed as JSON uses ""
                    val bodyLines = body.split("\n").map { s ->
                        "\"" + s.trim().replace("\"", "\\\"") + "\""
                    }

                    if (bodyLines.size == 1) {
                        lines.add(".body(${bodyLines.first()})")
                    } else {
                        lines.add(".body(${bodyLines.first()} + ")
                        lines.indent()
                        (1 until bodyLines.lastIndex).forEach { i ->
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

    private fun handleHeaders(call: ObjRestCallAction, lines: Lines) {

        val prechosenAuthHeaders = call.auth.headers.map { it.name }

        call.auth.headers.forEach {
            lines.add(".header(\"${it.name}\", \"${it.value}\") // ${call.auth.name}")
        }

        call.parameters.filterIsInstance<HeaderParam>()
                .filter { !prechosenAuthHeaders.contains(it.name) }
                .forEach {
                    lines.add(".header(\"${it.name}\", ${it.gene.getValueAsPrintableString(targetFormat = null)})")
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

    private fun assertReturnSimple(lines: Lines, result: RestCallResult){

        // .body("empty",equalTo(false))

        lines.add(".assertThat()")
        lines.indent(1)
        //if (result.failedCall() || result.getStatusCode() != 200){
        //   lines.add(".body(\"empty\", equalTo(true)")
        //}
        //else{
        //    lines.add(".body(\"type\", equalTo(\"${result.getBodyType()}\")")
        //}
        //TODO: note the code below is not actually okay. Test purposes only
        when{
            (result.getStatusCode() == 200) -> lines.add(".body(\"type\", equalTo(\"${result.getBodyType()}\")")
        //    (result.getStatusCode() != 200) -> lines.add(".body(\"empty\", equalTo(true)")
        }
        lines.deindent(1)
    }
}