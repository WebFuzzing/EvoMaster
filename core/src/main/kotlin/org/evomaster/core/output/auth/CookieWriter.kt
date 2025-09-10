package org.evomaster.core.output.auth

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.output.service.HttpWsTestCaseWriter
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.auth.EndpointCallLogin
import org.evomaster.core.problem.rest.data.ContentType
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual

/**
 * A test case might need to get cookies to do authenticated requests.
 * This means we need to first do a login/signup call to get cookies,
 * and store them somewhere in a variable
 */
object CookieWriter {

    fun cookiesName(info: EndpointCallLogin): String =
        TestWriterUtils.safeVariableName("cookies_${info.name}")

    /**
     *  Return the distinct auth info on cookie-based login in all actions
     *  of this individual
     */
    fun getCookieLoginAuth(ind: Individual) = ind.seeAllActions()
        .filterIsInstance<HttpWsAction>()
        .filter { it.auth.endpointCallLogin != null && it.auth.endpointCallLogin!!.expectsCookie() }
        .distinctBy { it.auth.name }
        .map { it.auth.endpointCallLogin!! }


    fun handleGettingCookies(
        format: OutputFormat,
        ind: EvaluatedIndividual<*>,
        lines: Lines,
        baseUrlOfSut: String,
        testCaseWriter: HttpWsTestCaseWriter
    ) {

        val cookiesInfo = getCookieLoginAuth(ind.individual)

        if (cookiesInfo.isNotEmpty()) {
            lines.addEmpty()
        }

        for (k in cookiesInfo) {

            when {
                format.isJava() -> lines.add("final Map<String,String> ${cookiesName(k)} = ")
                format.isKotlin() -> lines.add("val ${cookiesName(k)} : Map<String,String> = ")
                format.isJavaScript() -> lines.add("const ${cookiesName(k)} = ")
            }

            if (!format.isPython()) {
                // TODO: should we use DTO for cookie related requests?
                testCaseWriter.startRequest(lines)
                lines.indent()
            }

            val targetCookieVariable = when {
                /*
                 In python, cookies are returned in a CookieJar object which we will name cookies_foo_jar for example.
                 The CookieJar will then be converted to a dictionary that is passed on to the next request
                 in cookies=cookies_foo. Passing on the CookieJar to the next request did not seem to work.
                 */
                format.isPython() -> "${cookiesName(k)}_jar"
                else -> cookiesName(k)
            }

            addCallCommand(lines, k, testCaseWriter, format, baseUrlOfSut, targetCookieVariable)

            when {
                format.isJavaOrKotlin() -> lines.add(".then().extract().cookies()")
                format.isPython() -> lines.append(".cookies")
            }

            if(format.isJavaScript()){
                lines.add(".then((res) => res.headers['set-cookie'][0].split(';')[0])")
                lines.add(".catch((err) => (err.status >= 300 && err.status <= 399) ? err.response.headers['set-cookie'][0].split(';')[0] : null)")
                lines.appendSemicolon()
            }

            if (format.isPython()) {
                lines.add("${cookiesName(k)} = requests.utils.dict_from_cookiejar($targetCookieVariable)")
            }
            //TODO check response status and cookie headers?

            if(!format.isJavaScript()){
                lines.appendSemicolon()
            }

            lines.addEmpty()

            if (!format.isPython()) {
                lines.deindent()
            }
        }
    }



    fun addCallCommand(
        lines: Lines,
        k: EndpointCallLogin,
        testCaseWriter: HttpWsTestCaseWriter,
        format: OutputFormat,
        baseUrlOfSut: String,
        targetVariable: String
    ) {

        if(format.isJavaScript()) {
            callEndpoint(lines, k, format, baseUrlOfSut)
        }

        if(format.isPython()) {
            lines.add("headers = {}")
        }

        val contentType = k.contentType
        if(contentType != null) {
            when {
                format.isJavaOrKotlin() -> lines.add(".contentType(\"${contentType.defaultValue}\")")
                format.isJavaScript() -> lines.add(".set(\"content-type\", \"${contentType.defaultValue}\")")
                format.isPython() -> {
                    lines.add("headers[\"content-type\"] = \"${contentType.defaultValue}\"")
                }
            }

            when (contentType) {
                ContentType.X_WWW_FORM_URLENCODED -> {
                    val send = testCaseWriter.sendBodyCommand()
                    when {
                        format.isPython() -> lines.add("body = \"${k.payload}\"")
                        else -> lines.add(".$send(\"${k.payload}\")")
                    }
                }

                ContentType.JSON -> {
                    testCaseWriter.printSendJsonBody(k.payload!!, lines)
                }

                else -> {
                    throw IllegalStateException("Currently not supporting yet ${k.contentType} in login")
                }
            }
        }

        for(header in k.headers) {
            when {
                format.isJavaOrKotlin() -> lines.add(".header(\"${header.name}\", \"${header.value}\")")
                format.isJavaScript() -> lines.add(".set(\"${header.name}\", \"${header.value}\")")
                format.isPython() -> {
                    lines.add("headers[\"${header.name}\"] = \"${header.value}\"")
                }
            }
        }

        if (format.isJavaScript()){
            // disable redirections
            lines.add(".redirects(0)")
        }

        /*
            For RestAssure, the call to "post" must be last, which is in opposite of what
            needed in used libraries for Python and JS
         */
        if(format.isJavaOrKotlin()) {
            callEndpoint(lines, k, format, baseUrlOfSut)
        }

        if (format.isPython()) {
            lines.add("$targetVariable = requests \\")
            lines.indent(2)
            callEndpoint(lines, k, format, baseUrlOfSut)
            lines.append(", ")
            lines.indented {
                lines.add("headers=headers, data=body, allow_redirects=False)")
            }
            lines.deindent(2)
        }
    }

    private fun callEndpoint(
        lines: Lines,
        k: EndpointCallLogin,
        format: OutputFormat,
        baseUrlOfSut: String
    ) {
        val verb = k.verb.name.lowercase()
        lines.add(".$verb(")
        if (k.externalEndpointURL != null) {
            lines.append("\"${k.externalEndpointURL}\"")
        } else {
            when {
                format.isJava() || format.isJavaScript() -> lines.append("$baseUrlOfSut + \"")
                format.isPython() -> lines.append("self.$baseUrlOfSut + \"")
                else -> lines.append("\"\${$baseUrlOfSut}")
            }
            lines.append("${k.endpoint}\"")
        }
        if (!format.isPython()) {
            lines.append(")")
        }
    }
}
