package org.evomaster.e2etests.spring.openapi.v3.security.existenceleakage

import com.foo.rest.examples.spring.openapi.v3.security.existenceleakage.ExistenceLeakageParentNoExistenceController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SecurityExistenceLeakageNoExistenceEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ExistenceLeakageParentNoExistenceController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "SecurityExistenceLeakageNoExistenceEM",
                6000
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faultCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)

            assertFalse({ DefinedFaultCategory.SECURITY_EXISTENCE_LEAKAGE in faultCategories })
        }
    }
}
