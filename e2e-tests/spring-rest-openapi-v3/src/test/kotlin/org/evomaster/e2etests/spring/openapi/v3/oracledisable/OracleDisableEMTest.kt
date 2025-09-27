package org.evomaster.e2etests.spring.openapi.v3.oracledisable

import com.foo.rest.examples.spring.openapi.v3.security.oracledisable.OracleDisableController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class OracleDisableEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(OracleDisableController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "OracleDisableEM",
            600
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertEquals(3, faults.size)
            assertTrue(DefinedFaultCategory.SECURITY_EXISTENCE_LEAKAGE in faults)
            assertTrue(DefinedFaultCategory.SECURITY_WRONG_AUTHORIZATION in faults)
            assertTrue(DefinedFaultCategory.SECURITY_NOT_RECOGNIZED_AUTHENTICATED in faults)
        }
    }

    @Test
    fun testRunEMDisableAll() {
        runTestHandlingFlakyAndCompilation(
            "OracleDisableAll",
            600
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "schemaOracles", "false")
            setOption(args, "disabledOracleCodes", "204,205,206")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertEquals(0, faults.size)
            assertFalse(DefinedFaultCategory.SECURITY_EXISTENCE_LEAKAGE in faults)
            assertFalse(DefinedFaultCategory.SECURITY_WRONG_AUTHORIZATION in faults)
            assertFalse(DefinedFaultCategory.SECURITY_NOT_RECOGNIZED_AUTHENTICATED in faults)
        }
    }

    @Test
    fun testRunEMDisableOne() {
        runTestHandlingFlakyAndCompilation(
            "OracleDisableOne",
            600
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "schemaOracles", "false")
            setOption(args, "disabledOracleCodes", "204")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertEquals(2, faults.size)
            assertTrue(DefinedFaultCategory.SECURITY_WRONG_AUTHORIZATION in faults)
            assertTrue(DefinedFaultCategory.SECURITY_NOT_RECOGNIZED_AUTHENTICATED in faults)
        }
    }
}
