package org.evomaster.e2etests.spring.openapi.v3.httporacle.failmodification

import com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification.FailModificationController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class FailModificationEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(FailModificationController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "FailedModificationEM",
                500
        ) { args: MutableList<String> ->

            setOption(args, "security", "false")
            setOption(args, "schemaOracles", "false")
            setOption(args, "httpOracles", "true")
            setOption(args, "useExperimentalOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertEquals(1, faults.size)
            assertEquals(ExperimentalFaultCategory.HTTP_SIDE_EFFECTS_FAILED_MODIFICATION, faults.first())
        }
    }
}
