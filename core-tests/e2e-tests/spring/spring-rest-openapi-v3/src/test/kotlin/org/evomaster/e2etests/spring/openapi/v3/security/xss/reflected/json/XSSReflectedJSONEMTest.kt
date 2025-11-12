package org.evomaster.e2etests.spring.openapi.v3.security.xss.reflected.json

import com.foo.rest.examples.spring.openapi.v3.security.xss.reflected.json.XSSReflectedJSONController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class XSSReflectedJSONEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = false
            initClass(XSSReflectedJSONController(), config)
        }
    }

    @Test
    fun testXSSReflectedEM() {
        runTestHandlingFlakyAndCompilation(
            "XSSReflectedJSONEMTest",
            50,
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")


            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())

            val faultsCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)
            val faults = DetectedFaultUtils.getDetectedFaults(solution)

            assertTrue(DefinedFaultCategory.XSS in faultsCategories)

            assertTrue(faults.any {
                it.category == DefinedFaultCategory.XSS
                        && it.operationId == "POST:/api/reflected/json/comment"
            })

            assertTrue(faults.any {
                it.category == DefinedFaultCategory.XSS
                        && it.operationId == "GET:/api/reflected/json/search"
            })

            assertTrue(faults.any {
                it.category == DefinedFaultCategory.XSS
                        && it.operationId == "GET:/api/reflected/json/user/{username}"
            })
        }
    }
}
