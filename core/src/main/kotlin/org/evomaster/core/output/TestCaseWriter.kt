package org.evomaster.core.output

import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.EvaluatedAction


class TestCaseWriter {

    companion object {


        fun convertToCompilableTestCode(
                format: OutputFormat,
                test: TestCase,
                baseUrlOfSut: String)
                : Lines {

            val lines = Lines()

            if (format.isJUnit()) {
                lines.add("@Test")
            }

            when {
                format.isJava() -> lines.add("public void ${test.name}() throws Exception {")
                format.isKotlin() -> lines.add("fun ${test.name}()  {")
            }

            lines.indent()

            if (test.hasChainedLocations()) {
                lines.addEmpty()


                test.test.evaluatedActions()
                        .map { ea -> ea.action }
                        .filterIsInstance(RestCallAction::class.java)
                        .filter { a -> a.locationId != null}
                        .map{ a-> a.locationId}
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


        private fun locationVar(id: String): String {
            //TODO make sure name is syntactically valid
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

            if(res.failedCall()){
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
            lines.add("fail(\"Expected exception\");")
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

            handleAuth(call, lines)

            handleBody(call, lines)

            handleVerb(baseUrlOfSut, call, lines)

            handleResponse(lines, res)

            //finally, handle the last line(s)
            if (call.saveLocation && !res.stopping) {
                lines.add(".extract().header(\"location\");")
                lines.addEmpty()
                lines.deindent(2)

                lines.add("assertTrue(isValidURIorEmpty(${locationVar(call.path.lastElement())}));")
            } else {
                lines.append(";")
                lines.deindent(2)
            }
        }

        private fun handleFirstLine(call: RestCallAction, lines: Lines, res: RestCallResult) {
            lines.addEmpty()
            if (call.saveLocation && !res.stopping) {
                lines.append("${locationVar(call.path.lastElement())} = ")
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

                if(call.path.numberOfUsableQueryParams(call.parameters) <= 1 ) {
                    val uri = call.path.resolve(call.parameters)
                    lines.append("\"$uri\"")
                } else {
                    //several query parameters. lets have them one per line
                    val path = call.path.resolveOnlyPath(call.parameters)
                    val elements = call.path.resolveOnlyQuery(call.parameters)

                    lines.append("\"$path?\" + ")

                    lines.indent()
                    (0..elements.lastIndex-1).forEach{i -> lines.add("\"${elements[i]}&\" + ")}
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
            call.parameters.find { p -> p is BodyParam }
                    ?.let {
                        lines.add(".contentType(\"application/json\")")

                        val body = it.gene.getValueAsPrintableString()

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
            if(! form.isBlank()){
                lines.add(".contentType(\"application/x-www-form-urlencoded\")")
                lines.add(".body(\"$form\")")
            }
        }

        private fun handleAuth(call: RestCallAction, lines: Lines) {
            if (call.auth !is NoAuth) {
                call.auth.headers.forEach { h ->
                    lines.add(".header(\"${h.name}\", \"${h.value}\") // ${call.auth.name}")
                }
            }
        }

        private fun getAcceptHeader(): String{
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
}