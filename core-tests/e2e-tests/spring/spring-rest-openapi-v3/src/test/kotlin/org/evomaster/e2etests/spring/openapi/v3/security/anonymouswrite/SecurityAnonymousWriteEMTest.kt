package org.evomaster.e2etests.spring.openapi.v3.security.anonymouswrite

import com.foo.rest.examples.spring.openapi.v3.security.anonymouswrite.AnonymousWriteController
import com.foo.rest.examples.spring.openapi.v3.security.existenceleakage.ExistenceLeakageController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SecurityAnonymousWriteEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(AnonymousWriteController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "SecurityAnonymousWriteEM",
                100
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "schemaOracles", "false")
            setOption(args, "useExperimentalOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faultsCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)
            val faults = DetectedFaultUtils.getDetectedFaults(solution)

            assertEquals(1, faultsCategories.size)
            assertEquals(3, faults.size)

            assertTrue(ExperimentalFaultCategory.ANONYMOUS_WRITE in faultsCategories)

            // PUT:/api/resources/201/{id}
            assertTrue(faults.none {
                it.category == ExperimentalFaultCategory.ANONYMOUS_WRITE
                        && it.operationId == "PUT:/api/resources/201/{id}"
            })
        }
    }
}
