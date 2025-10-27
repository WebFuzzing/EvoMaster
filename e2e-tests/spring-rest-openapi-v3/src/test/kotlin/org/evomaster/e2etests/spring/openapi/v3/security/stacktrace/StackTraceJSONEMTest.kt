package org.evomaster.e2etests.spring.openapi.v3.security.stacktrace

import com.foo.rest.examples.spring.openapi.v3.security.stacktrace.StackTraceJSONController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class StackTraceJSONEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(StackTraceJSONController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "StackTraceJSONEM",
                100
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "schemaOracles", "false")
            setOption(args, "useExperimentalOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/resources/null-pointer-json", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/resources/null-pointer-json-not-list", null)

            val faultsCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)
            val faults = DetectedFaultUtils.getDetectedFaults(solution)

            assertTrue(ExperimentalFaultCategory.SECURITY_STACK_TRACE in faultsCategories)

            assertTrue(faults.any {
                it.category == ExperimentalFaultCategory.SECURITY_STACK_TRACE
                        && it.operationId == "GET:/api/resources/null-pointer-json"
            })

            assertTrue(faults.any {
                it.category == ExperimentalFaultCategory.SECURITY_STACK_TRACE
                        && it.operationId == "GET:/api/resources/null-pointer-json-not-list"
            })

        }
    }
}
