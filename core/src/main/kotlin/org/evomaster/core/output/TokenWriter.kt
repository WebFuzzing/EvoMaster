package org.evomaster.core.output

import org.evomaster.core.problem.httpws.service.HttpWsAction
import org.evomaster.core.problem.rest.ContentType
import org.evomaster.core.problem.rest.auth.JsonTokenPostLogin
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual

object TokenWriter {

    fun tokenName(info: JsonTokenPostLogin) = "token_${info.userId}"

    /**
     *  Return the distinct auth info on token-based login in all actions
     *  of this individual
     */
    fun getTokenLoginAuth(ind: Individual) =  ind.seeActions()
            .filterIsInstance<HttpWsAction>()
            .filter { it.auth.jsonTokenPostLogin != null }
            .map { it.auth.jsonTokenPostLogin!! }
            .distinctBy { it.userId }


    fun handleGettingTokens(format: OutputFormat,
                             ind: EvaluatedIndividual<*>,
                             lines: Lines,
                             baseUrlOfSut: String) {

        val tokensInfo = getTokenLoginAuth(ind.individual)

        if (tokensInfo.isNotEmpty()) {
            lines.addEmpty()
        }

        for (k in tokensInfo) {

            when {
                format.isJava() -> lines.add("final String ${tokenName(k)} = ")
                format.isKotlin() -> lines.add("val ${tokenName(k)} : String = ")
            }

            //TODO JS / C#

            lines.append("given()")
            lines.indented {

                lines.add(".body(\"${k.jsonPayload}\")")

                lines.add(".post(")
                if (format.isJava()) {
                    lines.append("$baseUrlOfSut + \"")
                } else {
                    lines.append("\"\${$baseUrlOfSut}")
                }
                lines.append("${k.endpoint}\")")

                //TODO find better way to convert from JSON Pointer (used in Jackson) to JsonPath (used in RestAssured)
                val path = k.extractTokenField.substring(1).replace("/",".")

                lines.add(".then().extract().response().path(\"$path\")")
                lines.appendSemicolon(format)
                lines.addEmpty()
            }
        }
    }
}