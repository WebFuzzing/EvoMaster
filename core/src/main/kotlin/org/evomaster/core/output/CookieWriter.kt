package org.evomaster.core.output

import org.evomaster.core.problem.rest.ContentType
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.auth.CookieLogin
import org.evomaster.core.search.EvaluatedIndividual


/**
 * A test case might need to get cookies to do authenticated requests.
 * This means we need to first do a login/signup call to get cookies,
 * and store them somewhere in a variable
 */
object CookieWriter {

    fun cookiesName(info: CookieLogin): String = "cookies_${info.username}"


    fun handleGettingCookies(format: OutputFormat,
                             ind: EvaluatedIndividual<*>,
                             lines: Lines,
                             baseUrlOfSut: String) {

        val cookiesInfo = (ind.individual as RestIndividual).getCookieLoginAuth()

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

                if (k.contentType == ContentType.X_WWW_FORM_URLENCODED) {
                    lines.add(".formParam(\"${k.usernameField}\", \"${k.username}\")")
                    lines.add(".formParam(\"${k.passwordField}\", \"${k.password}\")")
                } else {
                    throw IllegalStateException("Currently not supporting yet ${k.contentType} in login")
                }

                lines.add(".post(")
                if (format.isJava()) {
                    lines.append("$baseUrlOfSut + \"")
                } else {
                    lines.append("\"\${$baseUrlOfSut}")
                }
                lines.append("${k.loginEndpointUrl}\")")

                lines.add(".then().extract().cookies()") //TODO check response status and cookie headers?
                lines.appendSemicolon(format)
                lines.addEmpty()
            }
        }
    }
}