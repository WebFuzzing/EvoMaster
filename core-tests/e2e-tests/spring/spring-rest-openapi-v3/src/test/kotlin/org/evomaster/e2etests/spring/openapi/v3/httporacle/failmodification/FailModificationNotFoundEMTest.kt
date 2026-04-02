package org.evomaster.e2etests.spring.openapi.v3.httporacle.failmodification

import com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification.FailModificationForbiddenController
import com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification.FailModificationNotFoundController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class FailModificationNotFoundEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(FailModificationNotFoundController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "FailedModificationNotFoundEM",
                50
        ) { args: MutableList<String> ->

            setOption(args, "schemaOracles", "false")
            setOption(args, "httpOracles", "true")
            setOption(args, "useExperimentalOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faultsCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertTrue(ExperimentalFaultCategory.HTTP_SIDE_EFFECTS_FAILED_MODIFICATION in faultsCategories)
        }
    }
}
