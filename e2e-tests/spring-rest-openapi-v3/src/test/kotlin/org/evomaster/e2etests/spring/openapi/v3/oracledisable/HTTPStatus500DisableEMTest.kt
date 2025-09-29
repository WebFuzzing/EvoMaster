package org.evomaster.e2etests.spring.openapi.v3.oracledisable

import com.foo.rest.examples.spring.openapi.v3.schemavalidation.SchemaValidationController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.evomaster.notinstrumented.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HTTPStatus500DisableEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SchemaValidationController())
        }
    }

    @Test
    fun testRunEM_500() {

        runTestHandlingFlakyAndCompilation(
            "HTTPStatus500DisableEMTest",
            10
        ) { args: MutableList<String> ->

            Constants.statusCodeToReturn = 500

            setOption(args, "schemaOracles", "true")
            setOption(args, "disabledOracleCodes", DefinedFaultCategory.HTTP_STATUS_500.code.toString())

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/schemavalidation", null)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)

            assertEquals(1, faults.size)
            assertFalse(faults.contains(DefinedFaultCategory.HTTP_STATUS_500))
            assertTrue(faults.contains(DefinedFaultCategory.SCHEMA_INVALID_RESPONSE))
        }
    }


}
