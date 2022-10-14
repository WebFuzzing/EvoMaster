package org.evomaster.core.output.service

import org.evomaster.core.problem.external.service.httpws.HttpExternalServiceAction

/**
 * Helper functions for [TestCaseWriter] and [TestSuiteWriter]
 */
class TestWriterUtils {
    companion object {

        /**
         * Takes the [HttpExternalServiceAction] and generates a name to use for WireMock
         * server inside generated test cases.
         */
        fun getWireMockVariableName(action: HttpExternalServiceAction): String {
            return "wireMock__" + action
                .externalService
                .getSignature()
                .replace(".", "_")
        }
    }
}