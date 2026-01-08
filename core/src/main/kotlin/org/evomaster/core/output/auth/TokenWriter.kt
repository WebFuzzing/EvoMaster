package org.evomaster.core.output.auth

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.output.service.HttpWsTestCaseWriter
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.auth.EndpointCallLogin
import org.evomaster.core.problem.httpws.auth.TokenHandling
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual

object TokenWriter {

    fun tokenName(info: EndpointCallLogin) =
        TestWriterUtils.safeVariableName("token_${info.name}")

    fun authPayloadName(info: EndpointCallLogin) =
        TestWriterUtils.safeVariableName("auth_${info.name}")

    fun responseName(info: EndpointCallLogin): String =
        TestWriterUtils.safeVariableName("res_${info.name}")

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
                            testCaseWriter: HttpWsTestCaseWriter
    ) {

        val tokensInfo = getTokenLoginAuth(ind.individual)

        if (tokensInfo.isNotEmpty()) {
            lines.addEmpty()
        }

        for (k in tokensInfo) {

            val token = k.token!!

            when {
                format.isJava() -> lines.add("final String ${tokenName(k)} = ")
                format.isKotlin() -> lines.add("val ${tokenName(k)} : String = ")
                format.isJavaScript() -> lines.add("let ${tokenName(k)} = ")
            }

            when{
                format.isJavaOrKotlin() -> lines.append("given()")
                format.isJavaScript() -> {
                    lines.append("\"\"")
                    lines.appendSemicolon()
                    lines.addEmpty()
                    lines.append("await superagent")
                }
            }

            if (!format.isPython()) {
                lines.indent(2)
            }

            CookieWriter.addCallCommand(lines,k,testCaseWriter,format,baseUrlOfSut, responseName(k))

            var path = token.extractSelector.substring(1).replace("/",".")

            if (format.isPython()) {
                var endPath = ""
                path.split(".").forEach {
                    if (!it.startsWith("[")) {
                        endPath += "[\"$it\"]"
                    }
                }
                path = endPath
            }

            when(token.extractFrom){
                TokenHandling.ExtractFrom.BODY -> {
                    if (format.isJavaScript()) {
                        lines.add(".then(res => {${tokenName(k)} = res.body.$path;},")
                        lines.indented { lines.add("error => {console.log(error.response.body); throw Error(\"Auth failed.\")})") }
                    } else if (format.isPython()) {
                        lines.add("${tokenName(k)} =  ${responseName(k)}.json()$path")
                    }else if (format.isJavaOrKotlin()) {
                        lines.add(".then().extract().response().path(\"$path\")")
                        if(format.isKotlin()) {
                            lines.append("!!")
                        }
                    }
                    lines.appendSemicolon()
                }
                TokenHandling.ExtractFrom.HEADER -> {
                    val header = token.extractSelector
                    if (format.isJavaScript()) {
                        lines.add(".then(res => {${tokenName(k)} = res.get(\"$header\");},")
                        lines.indented { lines.add("error => {console.log(error.response.headers); throw Error(\"Auth failed.\")})") }
                    } else if (format.isPython()) {
                        lines.add("${tokenName(k)} =  ${responseName(k)}.headers[\"$header\"]")
                    }else if (format.isJavaOrKotlin()) {
                        lines.add(".then().extract().response().header(\"$header\")")
                        if(format.isKotlin()) {
                            lines.append("!!")
                        }
                    }
                    lines.appendSemicolon()
                }
            }
            lines.addEmpty()


            if (!format.isPython()) {
                lines.deindent(2)
            }

            when {
                format.isJava() -> lines.add("final String ${authPayloadName(k)} = ")
                format.isKotlin() -> lines.add("val ${authPayloadName(k)} : String = ")
                format.isJavaScript() -> lines.add("let ${authPayloadName(k)} = ")
                format.isPython() -> lines.add("${authPayloadName(k)} = ")
            }

            val default = TokenHandling.TOKEN_INTERPOLATION_TEMPLATE
            if(!token.sendTemplate.contains(default)){
                //is this even allowed? constant
                lines.append("\"${token.sendTemplate}\"")
                lines.appendSemicolon()
                lines.appendSingleCommentLine("{token} is not defined in the template")

            }else if(token.sendTemplate != default) {
                //normal case
                val prefix = token.sendTemplate.substringBefore(default)
                val postfix = token.sendTemplate.substringAfter(default)
                if(prefix.isNotEmpty()) {
                    lines.append("\"${prefix}\" + ")
                }
                lines.append(tokenName(k))
                if(postfix.isNotEmpty()) {
                    lines.append(" + \"${postfix}\"")
                }
                lines.appendSemicolon()

            }else{
                lines.append(tokenName(k))
                lines.appendSemicolon()
            }

        }
    }
}
