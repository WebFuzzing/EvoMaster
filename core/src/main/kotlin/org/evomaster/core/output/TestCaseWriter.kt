package org.evomaster.core.output

import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.EvaluatedAction


class TestCaseWriter {

    companion object {

        private val locationVarName = "location"


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
                when {
                    format.isJava() -> lines.add(padding(4) + "String $locationVarName = \"\";")
                    format.isKotlin() -> lines.add(padding(4) + "var $locationVarName = \"\"")
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

        private fun padding(n: Int): String {
            return when (n) {
                1 -> " "
                2 -> "  "
                3 -> "   "
                4 -> "    "
                5 -> "     "
                6 -> "      "
                7 -> "       "
                8 -> "        "
                9 -> "         "
                10 -> "          "
                11 -> "           "
                12 -> "            "
                else -> throw IllegalArgumentException("Invalid n=$n")
            }
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

            var firstLine = padding(4)
            if (call.locationChained && call.verb == HttpVerb.POST){
                firstLine += "$locationVarName = "
            }
            firstLine += "given()" + list[0]
            lines.add(firstLine)


            (1..list.lastIndex - 1).forEach { i ->
                lines.add(padding(12) + list[i])
            }

            if(call.locationChained && call.verb == HttpVerb.POST) {
                lines.add(padding(12) + list[list.lastIndex] )
                lines.add(padding(12) + ".extract().header(\"location\");")
                lines.add("")
                lines.add(padding(4) + "assertTrue(isValidURIorEmpty($locationVarName));")
            } else {
                lines.add(padding(12) + list[list.lastIndex]+ ";")
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
            if(call.locationChained && call.verb != HttpVerb.POST){
                callLine += "resolveLocation($locationVarName, $baseUrlOfSut + \"${call.path.toString()}\")"

            } else {
                val path = call.path.resolve(call.parameters)
                callLine += "$baseUrlOfSut + \"$path\""
            }
            callLine += ")"
            list.add(callLine)


            list.add(".then()")
            list.add(".statusCode(${res.getStatusCode()})")

            //TODO check on body

            return list
        }
    }
}