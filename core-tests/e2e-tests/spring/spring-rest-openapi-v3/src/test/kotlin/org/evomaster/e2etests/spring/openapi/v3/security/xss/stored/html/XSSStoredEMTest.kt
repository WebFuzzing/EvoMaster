package org.evomaster.e2etests.spring.openapi.v3.security.xss.stored.html

import com.foo.rest.examples.spring.openapi.v3.security.xss.stored.html.XSSStoredController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
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

            Assertions.assertTrue(solution.individuals.isNotEmpty())

            val faults = DetectedFaultUtils.getDetectedFaults(solution)

            Assertions.assertTrue(faults.size == 3)

            val faultCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)

            Assertions.assertTrue({ DefinedFaultCategory.XSS in faultCategories })

            Assertions.assertTrue(faults.any {
                it.category == DefinedFaultCategory.XSS
                        && it.operationId == "GET:/api/stored/comments"
            })

            Assertions.assertTrue(faults.any {
                it.category == DefinedFaultCategory.XSS
                        && it.operationId == "GET:/api/stored/guestbook"
            })

            Assertions.assertTrue(faults.any {
                it.category == DefinedFaultCategory.XSS
                        && it.operationId == "GET:/api/stored/user/{username}"
            })
        }
    }
}