package org.evomaster.core.output.auth

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.service.ApiTestCaseWriter
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.auth.EndpointCallLogin
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual

object TokenWriter {

    fun tokenName(info: EndpointCallLogin) = "token_${info.name}"

    /**
     *  Return the distinct auth info on token-based login in all actions
     *  of this individual
     */
    fun getTokenLoginAuth(ind: Individual) =  ind.seeAllActions()
            .filterIsInstance<HttpWsAction>()
            .filter { it.auth.endpointCallLogin != null && it.auth.endpointCallLogin!!.token!=null}
            .distinctBy { it.auth.name }
            .map { it.auth.endpointCallLogin!! }


    fun handleGettingTokens(format: OutputFormat,
                            ind: EvaluatedIndividual<*>,
                            lines: Lines,
                            baseUrlOfSut: String,
                            testCaseWriter: ApiTestCaseWriter
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

            if(k.token!!.headerPrefix.isNotEmpty()) {
                lines.append("\"${k.token!!.headerPrefix}\"")
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

            CookieWriter.addCallCommand(lines,k,testCaseWriter,format,baseUrlOfSut)

            val path = k.token!!.extractFromField.substring(1).replace("/",".")

            if (format.isJavaScript()){
                lines.add(".then(res => {${tokenName(k)} += res.body.$path;},")
                lines.indented { lines.add("error => {console.log(error.response.body); throw Error(\"Auth failed.\")});")}
            }else
                lines.add(".then().extract().response().path(\"$path\")")

            lines.appendSemicolon(format)
            lines.addEmpty()

            lines.deindent(2)

        }
    }
}