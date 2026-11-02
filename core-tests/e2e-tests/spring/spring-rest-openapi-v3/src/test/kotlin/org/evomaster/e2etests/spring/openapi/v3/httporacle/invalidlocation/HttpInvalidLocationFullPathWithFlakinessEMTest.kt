package org.evomaster.e2etests.spring.openapi.v3.httporacle.invalidlocation

import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.fullpath.HttpInvalidLocationFullPathController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HttpInvalidLocationFullPathWithFlakinessEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpInvalidLocationFullPathController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "HttpInvalidLocationFullPathWithFlakinessEM",
            20
        ) { args: MutableList<String> ->

            setOption(args, "security", "false")
            setOption(args, "schemaOracles", "false")
            setOption(args, "httpOracles", "true")
            setOption(args, "useExperimentalOracles", "true")

            // just for testing if the flakiness can be worked when httpOracles are enabled
            setOption(args, "handleFlakiness", "true")
            setOption(args, "execNumForDetectFlakiness", "2")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertTrue({ DefinedFaultCategory.HTTP_INVALID_LOCATION in faults })
        }
    }
}
