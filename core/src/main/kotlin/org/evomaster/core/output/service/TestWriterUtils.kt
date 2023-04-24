package org.evomaster.core.output.service

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.problem.externalservice.httpws.HttpWsExternalService
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction
import org.evomaster.core.problem.externalservice.httpws.param.HttpWsResponseParam
import org.evomaster.core.search.gene.utils.GeneUtils

/**
 * Helper functions for [TestCaseWriter] and [TestSuiteWriter]
 */
class TestWriterUtils {
    companion object {

        fun formatJsonWithEscapes(json: String, outputFormat: OutputFormat, extraSpace: String= " ") : List<String>{
            val body = if (OutputFormatter.JSON_FORMATTER.isValid(json)) {
                OutputFormatter.JSON_FORMATTER.getFormatted(json)
            } else {
                json
            }

            //needed as JSON uses ""
            return body.split("\n").map { s ->
                "\"$extraSpace" + GeneUtils.applyEscapes(s.trim(), mode = GeneUtils.EscapeMode.BODY, format = outputFormat) + "$extraSpace\""
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
        fun handleDefaultStubForAsJavaOrKotlin(lines: Lines, wm : HttpWsExternalService, outputFormat: OutputFormat){
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

        fun handleStubForAsJavaOrKotlin(lines: Lines, wm : HttpWsExternalService, response: HttpWsResponseParam, method: String, urlSetting: String, priority: Int, outputFormat: OutputFormat){
            val name = getWireMockVariableName(wm)
            val bodyLines = formatJsonWithEscapes(response.responseBody.getValueAsRawString(), outputFormat, extraSpace = "")
            handleStubForAsJavaOrKotlin(lines, name, method, urlSetting, response.connectionHeader, response.getResponseContentType(), response.status.getValueAsRawString().toInt(), bodyLines, priority)
        }

        private fun handleStubForAsJavaOrKotlin(lines: Lines, name: String, method: String, urlSetting: String, connectionHeader: String?, contentTypeHeader: String?, status: Int, bodyLines: List<String>, priority : Int){

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
                        if (bodyLines.size == 1){
                            lines.add(".withBody(${bodyLines.first()})")
                        }else if (bodyLines.size > 1){
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
    }
}