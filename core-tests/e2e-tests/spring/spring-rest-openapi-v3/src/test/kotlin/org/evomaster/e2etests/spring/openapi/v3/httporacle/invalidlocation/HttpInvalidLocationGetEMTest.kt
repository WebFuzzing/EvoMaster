package org.evomaster.e2etests.spring.openapi.v3.httporacle.invalidlocation

import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.locationget.HttpInvalidLocationGetController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

// TODO: RestCallAction.creationLocationId() currently restricts location-id generation
//  to POST/PUT and throws otherwise, so this branch silently no-ops on other verbs.
//  After that restriction is refactored to allow any verb whose response carried a
//  Location header, this can be activated
@Disabled
class HttpInvalidLocationGetEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpInvalidLocationGetController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpInvalidLocationGetEM",
                20
        ) { args: MutableList<String> ->

            setOption(args, "security", "false")
            setOption(args, "schemaOracles", "false")
            setOption(args, "httpOracles", "true")
            setOption(args, "useExperimentalOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertTrue({ ExperimentalFaultCategory.HTTP_INVALID_LOCATION in faults })
        }
    }
}
