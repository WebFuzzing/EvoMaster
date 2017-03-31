package org.evomaster.core.output

import org.evomaster.core.problem.rest.HttpVerb
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
                : List<String> {

            val lines: MutableList<String> = mutableListOf()

            if (format.isJUnit()) {
                lines.add("@Test")
            }

            when {
                format.isJava() -> lines.add("public void ${test.name}() throws Exception {")
                format.isKotlin() -> lines.add("fun ${test.name}()  {")
            }

            if (test.hasChainedLocations()) {
                lines.add("")

                test.test.evaluatedActions()
                        .map { ea -> ea.action }
                        .filterIsInstance(RestCallAction::class.java)
                        .filter { a -> a.locationId != null}
                        .map{ a-> a.locationId}
                        .distinct()
                        .forEach { id ->
                            val name = locationVar(id!!)
                            when {
                                format.isJava() -> lines.add(padding(4) + "String $name = \"\";")
                                format.isKotlin() -> lines.add(padding(4) + "var $name = \"\"")
                            }
                        }
            }


            test.test.evaluatedActions().forEach { a ->
                when (a.action) {
                    is RestCallAction -> handleRestCall(a, lines, baseUrlOfSut)
                    else -> throw IllegalStateException("Cannot handle " + a.action.getName())
                }
            }

            lines.add("}")

            return lines
        }

        private fun locationVar(id: String): String {
            //TODO make sure name is syntactically valid
            return "location_${id.trim().replace(" ", "_")}"
        }

        private fun padding(n: Int): String {

            if(n < 0) {
                throw IllegalArgumentException("Invalid n=$n")
            }

            val buffer = StringBuffer("")
            (0..n).forEach { buffer.append(" ") }
            return buffer.toString()
        }

        private fun handleRestCall(
                evaluatedAction: EvaluatedAction,
                lines: MutableList<String>,
                baseUrlOfSut: String
        ) {
            lines.add("")

            val call = evaluatedAction.action as RestCallAction
            val res = evaluatedAction.result as RestCallResult

            val list = restAssuredMethods(call, res, baseUrlOfSut)

            if(res.failedCall()){
                addRestCallInTryCatch(call, lines, list, res)
            } else {
                addRestCallLines(0, call, lines, list, res)
            }
        }

        private fun addRestCallInTryCatch(call: RestCallAction,
                                          lines: MutableList<String>,
                                          ram: MutableList<String>,
                                          res: RestCallResult) {

            lines.add(padding(4) + "try{")
            addRestCallLines(4, call, lines, ram, res)
            lines.add(padding(8) + "fail(\"Expected exception\");")
            lines.add(padding(4) + "} catch(Exception e){")
            res.getErrorMessage()?.let {
                lines.add(padding(8) + "//$it")
            }
            lines.add(padding(4) + "}")
        }

        private fun addRestCallLines(indentation: Int,
                                     call: RestCallAction,
                                     lines: MutableList<String>,
                                     /** RestAssured Methods */
                                     ram: MutableList<String>,
                                     res: RestCallResult) {

            //first handle the first line
            var firstLine = padding(indentation + 4)
            if (call.saveLocation && !res.stopping) {
                firstLine += "${locationVar(call.path.lastElement())} = "
            }
            firstLine += "given()" + ram[0]
            lines.add(firstLine)


            //then handle the lines between first and last (both excluded)
            (1..ram.lastIndex - 1).forEach { i ->
                lines.add(padding(indentation + 12) + ram[i])
            }


            //finall, handle the last line(s)
            if (call.saveLocation && !res.stopping) {
                lines.add(padding(indentation + 12) + ram[ram.lastIndex])
                lines.add(padding(indentation + 12) + ".extract().header(\"location\");")
                lines.add("")
                lines.add(padding(4) +
                        "assertTrue(isValidURIorEmpty(${locationVar(call.path.lastElement())}));")
            } else {
                lines.add(padding(indentation + 12) + ram[ram.lastIndex] + ";")
            }
        }


        private fun restAssuredMethods(
                call: RestCallAction,
                res: RestCallResult,
                baseUrlOfSut: String)
                : MutableList<String> {

            val list: MutableList<String> = mutableListOf()

            if (call.auth !is NoAuth) {
                call.auth.headers.forEach { h ->
                    list.add(".header(\"${h.name}\", \"${h.value}\") // ${call.auth.name}")
                }
            }

            /*
             *  Note: using the type in result body is wrong:
             *  if you request a JSON but make an error, you might
             *  get back a text/plain with an explanation
             *
             *  TODO: get the type from the REST call
             */
            list.add(".accept(\"*/*\")")

            call.parameters.find { p -> p is BodyParam }
                    ?.let {
                        list.add(".contentType(\"application/json\")")

                        val body = it.gene.getValueAsString()

                        val lines = body.split("\n").map { s ->
                            "\"" + s.trim().replace("\"", "\\\"") + "\""
                        }

                        if (lines.size == 1) {
                            list.add(".body(" + lines.first() + ")")
                        } else {

                            list.add(".body(" + lines.first() + " + ")
                            (1..lines.lastIndex - 1).forEach { i ->
                                list.add("      ${lines[i]} + ")
                            }
                            list.add("      ${lines.last()})")
                        }

                    }

            val verb = call.verb.name.toLowerCase()
            var callLine = ".$verb("
            if (call.locationId != null) {
                callLine += "resolveLocation(${locationVar(call.locationId!!)}, $baseUrlOfSut + \"${call.path.toString()}\")"

            } else {
                val path = call.path.resolve(call.parameters)
                callLine += "$baseUrlOfSut + \"$path\""
            }
            callLine += ")"
            list.add(callLine)

            if(! res.failedCall()) {
                list.add(".then()")
                list.add(".statusCode(${res.getStatusCode()})")

                //TODO check on body
            }
            return list
        }
    }
}