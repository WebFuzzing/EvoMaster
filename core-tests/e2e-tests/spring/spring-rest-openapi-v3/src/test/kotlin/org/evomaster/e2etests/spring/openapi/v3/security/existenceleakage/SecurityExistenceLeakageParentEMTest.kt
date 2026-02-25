package org.evomaster.e2etests.spring.openapi.v3.security.existenceleakage

import com.foo.rest.examples.spring.openapi.v3.security.existenceleakage.ExistenceLeakageParentController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SecurityExistenceLeakageParentEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ExistenceLeakageParentController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "SecurityExistenceLeakageParentEM",
                100
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.PUT, 201, "/api/parents/{pid}", null)
            assertHasAtLeastOne(solution, HttpVerb.PUT, 201, "/api/parents/{pid}/children/{cid}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 403, "/api/parents/{pid}/children/{cid}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/api/parents/{pid}/children/{cid}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 403, "/api/parents/{pid}", null)


            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertEquals(1, faults.size)
            assertEquals(DefinedFaultCategory.SECURITY_EXISTENCE_LEAKAGE, faults.first())
        }
    }
}
