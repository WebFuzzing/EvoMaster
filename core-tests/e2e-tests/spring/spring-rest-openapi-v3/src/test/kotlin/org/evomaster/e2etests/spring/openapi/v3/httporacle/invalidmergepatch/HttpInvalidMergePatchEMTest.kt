package org.evomaster.e2etests.spring.openapi.v3.httporacle.invalidmergepatch

import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidmergepatch.InvalidMergePatchController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HttpInvalidMergePatchEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(InvalidMergePatchController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpInvalidMergePatchEM",
                1000
        ) { args: MutableList<String> ->

            setOption(args, "security", "false")
            setOption(args, "schemaOracles", "false")
            setOption(args, "httpOracles", "true")
            setOption(args, "useExperimentalOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            // both endpoints must be exercised with a successful partial PATCH
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/api/mergepatch/buggy/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/api/mergepatch/correct/{id}", null)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertTrue(faults.contains(ExperimentalFaultCategory.HTTP_INVALID_MERGE_PATCH))

            val mergePatchFaults = DetectedFaultUtils.getDetectedFaults(solution)
                .filter { it.category == ExperimentalFaultCategory.HTTP_INVALID_MERGE_PATCH }
            // the buggy resource must be flagged...
            assertTrue(mergePatchFaults.any { it.operationId.contains("/api/mergepatch/buggy/") })
            // ...and the correct resource must NOT be flagged
            assertTrue(mergePatchFaults.none { it.operationId.contains("/api/mergepatch/correct/") })
        }
    }
}
