package org.evomaster.e2etests.spring.openapi.v3.security.stacktrace

import com.foo.rest.examples.spring.openapi.v3.security.stacktrace.StackTraceController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class StackTraceEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(StackTraceController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "StackTraceEM",
                100
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/resources/error", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/resources/divide/{a}/{b}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/resources/divide/{a}/{b}", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 500, "/api/resources/array-access/{index}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/resources/null-pointer", null)

            val faultsCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)
            val faults = DetectedFaultUtils.getDetectedFaults(solution)

            assertTrue(ExperimentalFaultCategory.SECURITY_STACK_TRACE in faultsCategories)

            // GET:/api/resources/null-pointer_not_stack_trace filter
            assertTrue(faults.none {
                it.category == ExperimentalFaultCategory.SECURITY_STACK_TRACE
                        && it.operationId == "GET:/api/resources/null-pointer_not_stack_trace"
            })
        }
    }
}
