package org.evomaster.e2etests.spring.openapi.v3.httporacle.failmodification

import com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification.FailModificationURLEncodedController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class URLEncodedFailModificationEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(FailModificationURLEncodedController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "URLEncodedFailedModificationEM",
                100
        ) { args: MutableList<String> ->

            setOption(args, "schemaOracles", "false")
            setOption(args, "httpOracles", "true")
            setOption(args, "useExperimentalOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faults = DetectedFaultUtils.getDetectedFaults(solution)

            assertEquals(1, faults.size)
            assertEquals(ExperimentalFaultCategory.HTTP_SIDE_EFFECTS_FAILED_MODIFICATION, faults.first().category)
        }
    }
}
