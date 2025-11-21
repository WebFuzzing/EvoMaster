package org.evomaster.e2etests.spring.openapi.v3.security.xss.stored

import com.foo.rest.examples.spring.openapi.v3.security.xss.stored.XSSStoredController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class XSSStoredEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = false
            initClass(XSSStoredController(), config)
        }
    }

    @Test
    fun testXSSStoredEM() {
        runTestHandlingFlakyAndCompilation(
            "XSSStoredEMTest",
            50,
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")


            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())

            val faults = DetectedFaultUtils.getDetectedFaults(solution)

            assertTrue(faults.size == 3)

            val faultCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)

            assertTrue({ DefinedFaultCategory.XSS in faultCategories })

            assertTrue(faults.any {
                it.category == DefinedFaultCategory.XSS
                        && it.operationId == "GET:/api/stored/comments"
            })

            assertTrue(faults.any {
                it.category == DefinedFaultCategory.XSS
                        && it.operationId == "GET:/api/stored/guestbook"
            })

            assertTrue(faults.any {
                it.category == DefinedFaultCategory.XSS
                        && it.operationId == "GET:/api/stored/user/{username}"
            })
        }
    }
}
