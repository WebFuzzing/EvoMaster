package org.evomaster.e2etests.spring.openapi.v3.security.xss.reflected.html

import com.foo.rest.examples.spring.openapi.v3.security.xss.reflected.html.XSSReflectedController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class XSSReflectedEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = false
            initClass(XSSReflectedController(), config)
        }
    }

    @Test
    fun testXSSReflectedEM() {
        runTestHandlingFlakyAndCompilation(
            "XSSReflectedEMTest",
            50,
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")


            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.isNotEmpty())

            val faultsCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)
            val faults = DetectedFaultUtils.getDetectedFaults(solution)

            Assertions.assertTrue(DefinedFaultCategory.XSS in faultsCategories)

            Assertions.assertTrue(faults.any {
                it.category == DefinedFaultCategory.XSS
                        && it.operationId == "POST:/api/reflected/comment"
            })

            Assertions.assertTrue(faults.any {
                it.category == DefinedFaultCategory.XSS
                        && it.operationId == "GET:/api/reflected/search"
            })

            Assertions.assertTrue(faults.any {
                it.category == DefinedFaultCategory.XSS
                        && it.operationId == "GET:/api/reflected/user/{username}"
            })
        }
    }
}