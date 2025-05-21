package org.evomaster.e2etests.spring.openapi.v3.security.forbiddendelete

import com.foo.rest.examples.spring.openapi.v3.security.forbiddendelete.SecurityForbiddenDeleteController
import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SecurityForbiddenDeleteEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SecurityForbiddenDeleteController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "SecurityForbiddenDeleteEM",
                100
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertEquals(1, faults.size)
            assertEquals(FaultCategory.SECURITY_FORBIDDEN_DELETE, faults.first())
        }
    }
}