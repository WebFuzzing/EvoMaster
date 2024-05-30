package org.evomaster.core.output.auth

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.service.ApiTestCaseWriter
import org.evomaster.core.output.service.HttpWsTestCaseWriter
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.auth.EndpointCallLogin
import org.evomaster.core.problem.rest.ContentType
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual

/**
 * A test case might need to get cookies to do authenticated requests.
 * This means we need to first do a login/signup call to get cookies,
 * and store them somewhere in a variable
 */
object CookieWriter {

    fun cookiesName(info: EndpointCallLogin): String = "cookies_${info.name}"


    /**
     *  Return the distinct auth info on cookie-based login in all actions
     *  of this individual
     */
    fun getCookieLoginAuth(ind: Individual) =  ind.seeAllActions()
            .filterIsInstance<HttpWsAction>()
            .filter { it.auth.endpointCallLogin != null && it.auth.endpointCallLogin!!.expectsCookie()}
            .distinctBy { it.auth.name }
            .map { it.auth.endpointCallLogin!! }


    fun handleGettingCookies(format: OutputFormat,
                             ind: EvaluatedIndividual<*>,
                             lines: Lines,
                             baseUrlOfSut: String,
                             testCaseWriter: ApiTestCaseWriter
    ) {

        val cookiesInfo =  getCookieLoginAuth(ind.individual)

        if (cookiesInfo.isNotEmpty()) {
            lines.addEmpty()
        }

        for (k in cookiesInfo) {

            when {
                format.isJava() -> lines.add("final Map<String,String> ${cookiesName(k)} = ")
                format.isKotlin() -> lines.add("val ${cookiesName(k)} : Map<String,String> = ")
            }

            //TODO JS

            lines.append("given()")
            lines.indented {
                addCallCommand(lines, k, testCaseWriter, format, baseUrlOfSut)
                lines.add(".then().extract().cookies()") //TODO check response status and cookie headers?
                lines.appendSemicolon()
                lines.addEmpty()
            }
        }
    }

     fun addCallCommand(
        lines: Lines,
        k: EndpointCallLogin,
        testCaseWriter: ApiTestCaseWriter,
        format: OutputFormat,
        baseUrlOfSut: String
    ) {
        //TODO check if payload is specified
        lines.add(".contentType(\"${k.contentType.defaultValue}\")")
        if (k.contentType == ContentType.X_WWW_FORM_URLENCODED) {
            if (testCaseWriter is HttpWsTestCaseWriter) { //FIXME
                val send = testCaseWriter.sendBodyCommand()
                lines.add(".$send(\"${k.payload}\")")
            }
        } else if (k.contentType == ContentType.JSON) {
            if (testCaseWriter is HttpWsTestCaseWriter) { //FIXME
                testCaseWriter.printSendJsonBody(k.payload, lines)
            }
        } else {
            throw IllegalStateException("Currently not supporting yet ${k.contentType} in login")
        }

         //TODO should check specified verb
        lines.add(".post(")
        if (k.externalEndpointURL != null) {
            lines.append("\"${k.externalEndpointURL}\")")
        } else {
            if (format.isJava()) {
                lines.append("$baseUrlOfSut + \"")
            } else {
                lines.append("\"\${$baseUrlOfSut}")
            }
            //TODO should check or guarantee that base does not end with a / ?
            lines.append("${k.endpoint}\")")
        }
    }
}
