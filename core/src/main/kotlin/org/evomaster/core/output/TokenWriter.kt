package org.evomaster.core.output

import org.evomaster.core.output.service.HttpWsTestCaseWriter
import org.evomaster.core.output.service.WebTestCaseWriter
import org.evomaster.core.problem.httpws.service.HttpWsAction
import org.evomaster.core.problem.httpws.service.auth.JsonTokenPostLogin
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.GeneUtils

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
                            baseUrlOfSut: String,
                            testCaseWriter: WebTestCaseWriter
    ) {

        val tokensInfo = getTokenLoginAuth(ind.individual)

        if (tokensInfo.isNotEmpty()) {
            lines.addEmpty()
        }

        for (k in tokensInfo) {

            when {
                format.isJava() -> lines.add("final String ${tokenName(k)} = ")
                format.isKotlin() -> lines.add("val ${tokenName(k)} : String = ")
                format.isJavaScript() -> lines.add("let ${tokenName(k)} = ")
            }


            // TODO C#

            if(k.headerPrefix.isNotEmpty()) {
                lines.append("\"${k.headerPrefix}\"")
            }else{
                if (format.isJavaScript())
                    lines.append("\"\"")
            }

            if (format.isJavaScript()){
                lines.appendSemicolon(format)
            }else{
                lines.append(" + ")
            }

            when{
                format.isJavaOrKotlin() -> lines.append("given()")
                format.isJavaScript() -> {
                    lines.addEmpty()
                    lines.append("await superagent")
                }
            }

            lines.indent(2)

            when{
                format.isJavaOrKotlin() -> {
                    lines.add(".contentType(\"application/json\")")
                }
                format.isJavaScript() -> {
                    appendPost(lines, baseUrlOfSut, format, k.endpoint)
                    lines.add(".set('Content-Type','application/json')")
                }
            }

            val json = k.jsonPayload

            if (testCaseWriter is HttpWsTestCaseWriter){
                testCaseWriter.printSendJsonBody(json, lines)
            }

            if (format.isJavaOrKotlin())
                appendPost(lines, baseUrlOfSut, format, k.endpoint)

            val path = k.extractTokenField.substring(1).replace("/",".")

            if (format.isJavaScript()){
                lines.add(".then(res => {${tokenName(k)} += res.body.$path;})")
            }else
                lines.add(".then().extract().response().path(\"$path\")")

            lines.appendSemicolon(format)
            lines.addEmpty()

            lines.deindent(2)

        }
    }


    private fun appendPost(lines: Lines, baseUrlOfSut: String, format: OutputFormat, endpoint: String){

        lines.add(".post(")

        if (format.isKotlin()) {
            lines.append("\"\${$baseUrlOfSut}")
        } else {
            lines.append("$baseUrlOfSut + \"")
        }

        lines.append("${endpoint}\")")
    }
}