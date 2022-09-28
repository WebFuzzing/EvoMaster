package org.evomaster.core.output.service

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.external.service.httpws.HttpExternalServiceAction

/**
 * Helper functions for [TestCaseWriter] and [TestSuiteWriter]
 */
class TestWriterUtils {
    companion object {
        /**
         * Check the current [EMConfig] and return boolean as the result
         */
         fun handleExternalService(config: EMConfig): Boolean {
            if (config.externalServiceIPSelectionStrategy != EMConfig.ExternalServiceIPSelectionStrategy.NONE) {
                return true
            }
            return false
        }

        /**
         * Takes the [HttpExternalServiceAction] and generates a name to use for WireMock
         * server inside generated test cases.
         */
        fun getWireMockVariableName(action: HttpExternalServiceAction): String {
            return action
                .externalService
                .externalServiceInfo
                .signature()
                .replace(".", "")
                .plus("WireMock")
        }
    }
}