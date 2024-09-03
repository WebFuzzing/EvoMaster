package org.evomaster.core.output

import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.problem.externalservice.httpws.HttpWsExternalService
import org.evomaster.core.problem.externalservice.httpws.param.HttpWsResponseParam
import org.evomaster.core.search.gene.utils.GeneUtils

/**
 * Helper functions for [TestCaseWriter] and [TestSuiteWriter]
 */
object TestWriterUtils {

    fun formatJsonWithEscapes(json: String, outputFormat: OutputFormat, extraSpace: String = " "): List<String> {
        val body = if (OutputFormatter.JSON_FORMATTER.isValid(json)) {
            OutputFormatter.JSON_FORMATTER.getFormatted(json)
        } else {
            json
        }

        //needed as JSON uses ""
        return body.split("\n").map { s ->
            "\"$extraSpace" + GeneUtils.applyEscapes(
                s.trim(),
                mode = GeneUtils.EscapeMode.BODY,
                format = outputFormat
            ) + "$extraSpace\""
        }
    }

    /**
     * Takes the [HttpExternalServiceAction] and generates a name to use for WireMock
     * server inside generated test cases.
     */
    fun getWireMockVariableName(externalService: HttpWsExternalService): String {
        return "wireMock__" + externalService.getSignature().replace(".", "_").replace("-", "_")
    }

    /**
     * generate scripts to configure the default settings for WM
     */
    fun handleDefaultStubForAsJavaOrKotlin(lines: Lines, wm: HttpWsExternalService, outputFormat: OutputFormat) {
        val name = getWireMockVariableName(wm)
        handleStubForAsJavaOrKotlin(
            lines,
            name,
            wm.getWMDefaultMethod(),
            wm.getWMDefaultUrlSetting(),
            wm.getWMDefaultConnectionHeader(),
            wm.getWMDefaultContentTypeHeader(),
            wm.getWMDefaultCode(),
            formatJsonWithEscapes(wm.getWMDefaultMessage(), outputFormat, extraSpace = ""),
            wm.getWMDefaultPriority()
        )
    }

    fun handleStubForAsJavaOrKotlin(
        lines: Lines,
        wm: HttpWsExternalService,
        response: HttpWsResponseParam,
        method: String,
        urlSetting: String,
        priority: Int,
        outputFormat: OutputFormat
    ) {
        val name = getWireMockVariableName(wm)
        val bodyLines = formatJsonWithEscapes(response.getResponseBodyBasedOnStatus(), outputFormat, extraSpace = "")
        handleStubForAsJavaOrKotlin(
            lines,
            name,
            method,
            urlSetting,
            response.connectionHeader,
            response.getResponseContentType(),
            response.getHttpStatusCode(),
            bodyLines,
            priority
        )
    }

    private fun handleStubForAsJavaOrKotlin(
        lines: Lines,
        name: String,
        method: String,
        urlSetting: String,
        connectionHeader: String?,
        contentTypeHeader: String?,
        status: Int,
        bodyLines: List<String>,
        priority: Int
    ) {

        lines.add("${name}.stubFor(")
        lines.indented {
            lines.add(
                "$method($urlSetting)"
            )
            // adding priority from the index of the respective action
            // TODO: when handling multiple calls need to fix this
            lines.add(".atPriority(${priority})")
            lines.add(".willReturn(")
            lines.indented {
                lines.add("aResponse()")
                lines.indented {
                    if (connectionHeader != null)
                        lines.add(".withHeader(\"Connection\",\"${connectionHeader}\")")
                    if (contentTypeHeader != null)
                        lines.add(".withHeader(\"Content-Type\",\"${contentTypeHeader}\")")
                    lines.add(".withStatus(${status})")
                    if (bodyLines.size == 1) {
                        lines.add(".withBody(${bodyLines.first()})")
                    } else if (bodyLines.size > 1) {
                        lines.add(".withBody(${bodyLines.first()} +")
                        lines.indented {
                            (1 until bodyLines.lastIndex).forEach { i ->
                                lines.add("${bodyLines[i]} + ")
                            }
                            lines.add(bodyLines.last())
                        }
                        lines.add(")")
                    }
                }
                lines.add(")")
            }
        }
        lines.add(")")
    }

    /**
     * Some character shouldn't be used for variable names, as it would lead to compilation/runtime errors.
     * Those are replaced with safe ones
     */
    fun safeVariableName(name: String): String {

        val safe = name.replace(Regex("[^0-9a-zA-Z_]"), "_")
        val first = safe.codePointAt(0)
        if(first >= '0'.code && first <= '9'.code ) {
            //can't start a variable name with a number
            return "_$safe"
        }
        return safe
    }

    private const val START_QUOTE_PATTERN = "\" \\\""
    private const val END_QUOTE_PATTERN = "\\\" \""
    private const val REPLACEMENT_QUOTE = "\""

    /**
     * Processes a list of strings representing lines of a JSON body to remove extraneous
     * starting and ending quotes from the first and last lines, if they are present.
     *
     * This function checks if the first line starts with a specific quote pattern and if the
     * last line ends with another quote pattern. If both conditions are met, it removes the
     * extra quotes from these lines and returns a new list with the modified lines. If the
     * conditions are not met, the function returns the original list unchanged.
     *
     * @param originalBodyLines The list of strings representing lines of the JSON body.
     * @return A new list of strings with the extraneous quotes removed from the first and
     *         last lines, or the original list if no quotes were removed.
     *
     * @see START_QUOTE_PATTERN for the pattern used to identify starting quotes.
     * @see END_QUOTE_PATTERN for the pattern used to identify ending quotes.
     * @see REPLACEMENT_QUOTE for the replacement quote used in the modification.
     */
    fun removeStartingAndEndingQuotesInJsonBody(originalBodyLines: List<String>):List<String> {
        if (originalBodyLines.isEmpty()) {
            return originalBodyLines
        } else {
            val isFirstLineQuoted = originalBodyLines.first().startsWith(START_QUOTE_PATTERN)
            val isLastLineQuoted = originalBodyLines.last().endsWith(END_QUOTE_PATTERN)

            if (isFirstLineQuoted && isLastLineQuoted) {
                val modifiedBodyLines = originalBodyLines.toMutableList()

                // Remove extra quotes from the first line
                modifiedBodyLines[0] = modifiedBodyLines[0].replaceFirst(START_QUOTE_PATTERN, REPLACEMENT_QUOTE)
                // Remove extra quotes from the last line
                modifiedBodyLines[modifiedBodyLines.size - 1] = modifiedBodyLines[modifiedBodyLines.size - 1].substring(
                    0,
                    modifiedBodyLines[modifiedBodyLines.size - 1].lastIndexOf(END_QUOTE_PATTERN)
                ) + REPLACEMENT_QUOTE

                return modifiedBodyLines.toList()
            } else {
                return originalBodyLines
            }
        }
    }
}
