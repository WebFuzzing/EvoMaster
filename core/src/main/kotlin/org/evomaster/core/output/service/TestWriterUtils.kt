package org.evomaster.core.output.service

import org.evomaster.core.output.Lines
import org.evomaster.core.problem.external.service.httpws.HttpWsExternalService
import org.evomaster.core.problem.external.service.httpws.HttpExternalServiceAction
import org.evomaster.core.problem.external.service.httpws.param.HttpWsResponseParam

/**
 * Helper functions for [TestCaseWriter] and [TestSuiteWriter]
 */
class TestWriterUtils {
    companion object {

        /**
         * Takes the [HttpExternalServiceAction] and generates a name to use for WireMock
         * server inside generated test cases.
         */
        fun getWireMockVariableName(externalService: HttpWsExternalService): String {
            return "wireMock__" + externalService.getSignature().replace(".", "_")
        }

        /**
         * generate scripts to configure the default settings for WM
         */
        fun handleDefaultStubForAsJavaOrKotlin(lines: Lines, wm : HttpWsExternalService){
            val name = getWireMockVariableName(wm)
            handleStubForAsJavaOrKotlin(
                lines,
                name,
                wm.getWMDefaultMethod(),
                wm.getWMDefaultUrlSetting(),
                wm.getWMDefaultConnectionHeader(),
                wm.getWMDefaultCode(),
                wm.getWMDefaultMessage(),
                wm.getWMDefaultPriority()
            )
        }

        fun handleStubForAsJavaOrKotlin(lines: Lines, wm : HttpWsExternalService, response: HttpWsResponseParam, method: String, urlSetting: String, priority: Int){
            val name = getWireMockVariableName(wm)
            handleStubForAsJavaOrKotlin(lines, name, method, urlSetting, response.connectionHeader, response.status.getValueAsRawString().toInt(), response.responseBody.getValueAsRawString().replace("\"", "\\\""), priority)
        }

        private fun handleStubForAsJavaOrKotlin(lines: Lines, name: String, method: String, urlSetting: String, connectionHeader: String?, status: Int, body: String, priority : Int){

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
                        lines.add(".withStatus(${status})")
                        //TODO possible need to handle type, eg JSON vs XML
                        //FIXME need major refactoring of escaping
                        lines.add(".withBody(\"${body}\")")
                    }
                    lines.add(")")
                }
            }
            lines.add(")")
        }
    }
}