package org.evomaster.e2etests.spring.openapi.v3.regexbody

import com.foo.rest.examples.spring.openapi.v3.regexbody.RegexBodyController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class RegexBodyEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(RegexBodyController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "RegexBodyEM",
                "org.foo.RegexBodyEM",
                100
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/regexbody", "OK")
            assertNone(solution, HttpVerb.POST, 500)
        }
    }
}