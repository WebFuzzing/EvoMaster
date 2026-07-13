package org.evomaster.core.output.auth

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.service.HttpWsTestCaseWriter
import org.evomaster.core.problem.httpws.auth.CallToEndpoint
import org.evomaster.core.problem.httpws.auth.PlaceHolderResolver
import org.evomaster.core.problem.rest.data.ContentType
import org.evomaster.core.search.gene.utils.GeneUtils

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
        targetVariable: String?,
        placeHolderResolver: PlaceHolderResolver?
    ) {

        // Compute placeholder replacements for PLaywright (used for dynamic user data)
        if (format.isPlaywright()) {
            val replacements: List<String>? = placeHolderResolver?.placeHolders?.entries?.map {
                val placeholder = GeneUtils.applyEscapes(it.key, mode = GeneUtils.EscapeMode.BODY, format)
                ".replace(\"${placeholder}\", ${it.value})"
            }
            val verb = k.verb.name.lowercase()
            lines.add(".$verb(")
            if (k.externalEndpointURL != null) {
                lines.append("\"${k.externalEndpointURL}\"")
            } else {
                lines.append("$baseUrlOfSut + \"")
                lines.append("${k.endpoint}\"")
            }
            lines.append(", {")
            lines.addEmpty()
            lines.indented {
                // headers block
                lines.add("headers: {")
                lines.indented {
                    // content-type header if present
                    val contentType = k.contentType
                    if (contentType != null) {
                        lines.add("'content-type': \"${contentType.defaultValue}\",")
                    }
                    // any additional headers
                    for (header in k.headers) {
                        lines.add("'${header.name}': \"${header.value}\",")
                    }
                }
                lines.add("},")

                // body/payload
                val contentType = k.contentType
                if (contentType != null) {
                    when (contentType) {
                        ContentType.X_WWW_FORM_URLENCODED -> {
                            if (replacements == null) {
                                lines.add("data: \"${k.payload}\",")
                            } else {
                                lines.add("data: \"${k.payload}\"")
                                // Apply string replacements for placeholders
                                replacements.forEach { lines.append(it) }
                                lines.append(",")
                            }
                        }
                        ContentType.JSON -> {
                            testCaseWriter.printSendJsonBody(k.payload!!, lines, functionsOnString = replacements)
                            // printSendJsonBody for Playwright appends a trailing comma
                        }
                        else -> throw IllegalStateException("Currently not supporting yet ${k.contentType} in login")
                    }
                }

                // disable redirections and ignore HTTPS errors
                lines.add("maxRedirects: 0,")
                lines.add("ignoreHTTPSErrors: true,")
            }
            lines.add("})")
            return
        }

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

            val replacements = placeHolderResolver?.placeHolders?.entries?.map {
                    val placeholder = GeneUtils.applyEscapes(it.key, mode = GeneUtils.EscapeMode.BODY, format)
                    ".replace(\"${placeholder}\", ${it.value})"
            }

            when (contentType) {
                ContentType.X_WWW_FORM_URLENCODED -> {
                    val send = testCaseWriter.sendBodyCommand()
                    if(replacements == null) {
                        when {
                            format.isPython() -> lines.add("body = \"${k.payload}\"")
                            else -> lines.add(".$send(\"${k.payload}\")")
                        }
                    } else {
                        when {
                            format.isPython() -> {
                                lines.add("body = \"${k.payload}\"")
                                replacements.forEach { lines.append(it) }
                            }
                            else -> {
                                lines.add(".$send(\"${k.payload}\"")
                                replacements.forEach { lines.append(it) }
                                lines.append(")")
                            }
                        }
                    }
                }

                ContentType.JSON -> {
                    testCaseWriter.printSendJsonBody(k.payload!!, lines, functionsOnString = replacements)
                }

                else -> {
                    throw IllegalStateException("Currently not supporting yet ${k.contentType} in auth handling")
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

        // Disable redirections where supported (SuperAgent/Frisby style only)
        if (format.isJavaScript() && !format.isPlaywright()){
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