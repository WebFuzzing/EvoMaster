package org.evomaster.core.output.auth

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.service.HttpWsTestCaseWriter
import org.evomaster.core.problem.httpws.auth.CallToEndpoint
import org.evomaster.core.problem.rest.data.ContentType

object AuthWriter {

    /**
     * Add lines related to make the call (eg setup of body payload), without the opening function (eg, 'given()' for
     * RestAssured).
     * Python is treated specially, as before the opening function we need to setup some variables.
     * The opening function is then added here in this function.
     *
     * @param lines Current lines buffer
     * @param k The endpoint to call
     * @param targetVariable Only used for languages like Python. If present, in the  generated code the result of call
     *                       is saved to this variable.
     */
    fun addBodyOfCallCommand(
        lines: Lines,
        k: CallToEndpoint,
        testCaseWriter: HttpWsTestCaseWriter,
        format: OutputFormat,
        baseUrlOfSut: String,
        targetVariable: String?
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
            if(targetVariable != null){
                lines.add("$targetVariable = requests \\")
            } else {
                lines.add("requests \\")
            }
            lines.indent(2)
            callEndpoint(lines, k, format, baseUrlOfSut)
            lines.append(", ")
            lines.indented {
                lines.add("headers=headers, data=body, allow_redirects=False, verify=False)")
            }
            lines.deindent(2)
        }
    }

    private fun callEndpoint(
        lines: Lines,
        k: CallToEndpoint,
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