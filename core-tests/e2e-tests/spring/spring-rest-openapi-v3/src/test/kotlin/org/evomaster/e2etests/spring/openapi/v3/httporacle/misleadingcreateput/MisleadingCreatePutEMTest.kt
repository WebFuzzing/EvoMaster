package org.evomaster.e2etests.spring.openapi.v3.httporacle.misleadingcreateput

import com.foo.rest.examples.spring.openapi.v3.httporacle.misleadingcreateput.HttpMisleadingCreatePutController
import com.foo.rest.examples.spring.openapi.v3.httporacle.partialupdateput.HttpPartialUpdatePutController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class MisleadingCreatePutEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpMisleadingCreatePutController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpMisleadingCreatePutPutEM",
                3000
        ) { args: MutableList<String> ->

            setOption(args, "security", "false")
            setOption(args, "schemaOracles", "false")
            setOption(args, "httpOracles", "true")
            setOption(args, "useExperimentalOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.PUT, 201, "/api/resources/{id}", null)

            val faultsCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertTrue(ExperimentalFaultCategory.HTTP_MISLEADING_CREATE_PUT in faultsCategories)

        }
    }
}
