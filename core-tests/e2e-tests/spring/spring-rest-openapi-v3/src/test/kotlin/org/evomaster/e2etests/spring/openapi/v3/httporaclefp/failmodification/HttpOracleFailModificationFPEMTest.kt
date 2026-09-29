package org.evomaster.e2etests.spring.openapi.v3.httporaclefp.failmodification

import com.foo.rest.examples.spring.openapi.v3.httporaclefp.deletefp.HttpOracleDeleteFPController
import com.foo.rest.examples.spring.openapi.v3.httporaclefp.failmodificationfp.HttpOracleFailModificationFPController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HttpOracleFailModificationFPEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpOracleFailModificationFPController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpOracleFailModificationFPEM",
                200
        ) { args: MutableList<String> ->

            setOption(args, "httpOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.PUT, 400, "/api/resources", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/resources", null)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertFalse( DefinedFaultCategory.HTTP_SIDE_EFFECTS_FAILED_MODIFICATION in faults )
        }
    }
}
