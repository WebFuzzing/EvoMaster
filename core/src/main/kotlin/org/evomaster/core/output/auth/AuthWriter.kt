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

        // Playwright specific handling
        if (format.isPlaywright()) {
            addPlaywrightAuthCall(lines, k, testCaseWriter, format, baseUrlOfSut, placeHolderResolver)
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

            val replacements = buildReplacements(format, placeHolderResolver)

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

    /**
     * Emit Playwright request call for authentication endpoints, including headers, body and options.
     */
    private fun addPlaywrightAuthCall(
        lines: Lines,
        k: CallToEndpoint,
        testCaseWriter: HttpWsTestCaseWriter,
        format: OutputFormat,
        baseUrlOfSut: String,
        placeHolderResolver: PlaceHolderResolver?
    ) {
        // Compute placeholder replacements for Playwright (used for dynamic user data)
        val replacements: List<String>? = buildReplacements(format, placeHolderResolver)

        // Hoist contentType once and reuse below (headers + body)
        val contentType = k.contentType

        // Emit verb and URL, leave call open to add Playwright options object
        emitVerbAndUrl(lines, k, format, baseUrlOfSut, close = false)
        lines.append(", {")
        lines.addEmpty()
        lines.indented {
            emitPlaywrightHeaders(lines, k, contentType)
            emitPlaywrightBody(lines, k, testCaseWriter, replacements, contentType)
            emitPlaywrightRequestOptions(lines)
        }
        lines.add("})")
    }

    /**
     * Emit the Playwright headers block, including optional content-type and any custom headers.
     */
    private fun emitPlaywrightHeaders(
        lines: Lines,
        k: CallToEndpoint,
        contentType: ContentType?
    ) {
        lines.add("headers: {")
        lines.indented {
            if (contentType != null) {
                lines.add("'content-type': \"${contentType.defaultValue}\",")
            }
            for (header in k.headers) {
                lines.add("'${header.name}': \"${header.value}\",")
            }
        }
        lines.add("},")
    }

    /**
     * Emit the Playwright body/payload block according to the content type.
     */
    private fun emitPlaywrightBody(
        lines: Lines,
        k: CallToEndpoint,
        testCaseWriter: HttpWsTestCaseWriter,
        replacements: List<String>?,
        contentType: ContentType?
    ) {
        if (contentType == null) return
        when (contentType) {
            ContentType.X_WWW_FORM_URLENCODED -> {
                if (replacements == null) {
                    lines.add("data: \"${k.payload}\",")
                } else {
                    lines.add("data: \"${k.payload}\"")
                    replacements.forEach { lines.append(it) }
                    lines.append(",")
                }
            }
            ContentType.JSON -> {
                testCaseWriter.printSendJsonBody(k.payload!!, lines, functionsOnString = replacements)
            }
            else -> throw IllegalStateException("Currently not supporting yet ${k.contentType} in login")
        }
    }

    /**
     * Emit standard Playwright request options for auth requests.
     */
    private fun emitPlaywrightRequestOptions(lines: Lines) {
        lines.add("maxRedirects: 0,")
        lines.add("ignoreHTTPSErrors: true,")
    }

    /**
     * Emit the HTTP verb and URL for the call, with an option to close the parenthesis.
     * Example (Java/JS): `.post(baseUrl + "/login"` and closes with `)` if `close=true`.
     */
    private fun emitVerbAndUrl(
        lines: Lines,
        k: CallToEndpoint,
        format: OutputFormat,
        baseUrlOfSut: String,
        close: Boolean = false
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
        if (close) {
            lines.append(")")
        }
    }

    /**
     * Build a list of string replacement functions to apply to payload strings.
     * Each entry is a snippet like: `.replace("<placeholder>", value)`; returns null if no placeholders.
     */
    private fun buildReplacements(
        format: OutputFormat,
        placeHolderResolver: PlaceHolderResolver?
    ): List<String>? = placeHolderResolver?.placeHolders?.entries?.map {
        val placeholder = GeneUtils.applyEscapes(it.key, mode = GeneUtils.EscapeMode.BODY, format)
        ".replace(\"${placeholder}\", ${it.value})"
    }

    private fun callEndpoint(
        lines: Lines,
        k: CallToEndpoint,
        format: OutputFormat,
        baseUrlOfSut: String
    ) {
        // Delegate to the shared emitter; Python keeps it open, others close it
        emitVerbAndUrl(lines, k, format, baseUrlOfSut, close = !format.isPython())
    }
}