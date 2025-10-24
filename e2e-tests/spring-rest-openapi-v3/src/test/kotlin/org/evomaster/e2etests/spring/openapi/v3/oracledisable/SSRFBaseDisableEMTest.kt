package org.evomaster.e2etests.spring.openapi.v3.oracledisable

import com.foo.rest.examples.spring.openapi.v3.security.ssrf.base.SSRFBaseController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.EMConfig
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SSRFBaseDisableEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = false
            initClass(SSRFBaseController(), config)
        }
    }

    @Test
    fun testSSRFEM() {
        runTestHandlingFlakyAndCompilation(
            "SSRFBaseDisableEMTest",
            50,
        ) { args: MutableList<String> ->

            // If mocking enabled, it'll spin new services each time when there is a valid URL.
            setOption(args, "externalServiceIPSelectionStrategy", "NONE")

            setOption(args, "security", "true")
            setOption(args, "ssrf", "true")
            setOption(args, "vulnerableInputClassificationStrategy", "MANUAL")

            setOption(args, "languageModelConnector", "false")
            setOption(args, "schemaOracles", "false")
            setOption(args, "disabledOracleCodes", DefinedFaultCategory.SSRF.code.toString())

            // TODO: Remove once EnumGene is supported for DTOs
            setOption(args, "dtoForRequestPayload","false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())
            assertFalse { solution.hasSsrfFaults() }
        }
    }
}
