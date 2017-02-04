package org.evomaster.core.output

import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
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
            lines.add("")

            test.test.evaluatedActions().forEach { a ->
                when (a.action) {
                    is RestCallAction -> handleRestCall(a, lines, baseUrlOfSut)
                    else -> throw IllegalStateException("Cannot handle " + a.action.getName())
                }
            }

            lines.add("}")

            return lines
        }


        private fun handleRestCall(
                evaluatedAction: EvaluatedAction,
                lines: MutableList<String>,
                baseUrlOfSut: String
        ) {

            val call = evaluatedAction.action as RestCallAction
            val res = evaluatedAction.result as RestCallResult

            val list = restAssureMethods(call, res, baseUrlOfSut)

            lines.add("    given()" + list[0])
            (1..list.lastIndex - 1).forEach { i ->
                lines.add("            " + list[i])
            }
            lines.add("            " + list[list.lastIndex] + ";")
        }

        private fun restAssureMethods(
                call: RestCallAction,
                res: RestCallResult,
                baseUrlOfSut: String)
                : MutableList<String> {

            val list: MutableList<String> = mutableListOf()

            res.getBodyType()?.let {
                list.add(".accept(\"$it\")")
            }

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
            val path = call.path.resolve(call.parameters)
            list.add(".$verb($baseUrlOfSut + \"$path\")")

            list.add(".then()")
            list.add(".statusCode(${res.getStatusCode()})")

            //TODO check on body

            return list
        }
    }
}