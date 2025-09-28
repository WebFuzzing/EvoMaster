package org.evomaster.e2etests.spring.openapi.v3.oracledisable

import com.foo.rest.examples.spring.openapi.v3.security.forgottenauthentication.ForgottenAuthenticationController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ForgottenAuthenticationDisableEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ForgottenAuthenticationController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "ForgottenAuthenticationDisableEM",
                100
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "schemaOracles", "false")
            setOption(args, "disabledOracleCodes", "980")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertFalse(ExperimentalFaultCategory.SECURITY_FORGOTTEN_AUTHENTICATION in faults)

        }
    }
}
