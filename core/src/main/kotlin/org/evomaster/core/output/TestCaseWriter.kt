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

            val list = restAssuredMethods(call, res, baseUrlOfSut)

            if(res.failedCall()){
                addRestCallInTryCatch(call, lines, list, res)
            } else {
                addRestCallLines(call, lines, list, res)
            }
        }

        private fun addRestCallInTryCatch(call: RestCallAction,
                                          lines: Lines,
                                          ram: MutableList<String>,
                                          res: RestCallResult) {

            lines.add("try{")
            lines.indent()

            addRestCallLines(call, lines, ram, res)
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
                                     /** RestAssured Methods */
                                     ram: MutableList<String>,
                                     res: RestCallResult) {

            //first handle the first line
            var firstLine = ""
            if (call.saveLocation && !res.stopping) {
                firstLine += "${locationVar(call.path.lastElement())} = "
            }
            firstLine += "given()" + ram[0]
            lines.add(firstLine)


            lines.indent()
            //then handle the lines after the first, till the last (included)
            (1..ram.lastIndex).forEach { i ->
                lines.add(ram[i])
            }


            //finally, handle the last line(s)
            if (call.saveLocation && !res.stopping) {
                lines.add(".extract().header(\"location\");")
                lines.addEmpty()
                lines.deindent()

                lines.add("assertTrue(isValidURIorEmpty(${locationVar(call.path.lastElement())}));")
            } else {
                lines.append(";")
                lines.deindent()
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