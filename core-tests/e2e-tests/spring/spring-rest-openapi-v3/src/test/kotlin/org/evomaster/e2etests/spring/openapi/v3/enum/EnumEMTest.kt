package org.evomaster.e2etests.spring.openapi.v3.enum

import com.foo.rest.examples.spring.openapi.v3.enum.EnumController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class EnumEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(EnumController())
        }
    }

    @Test
    fun testRunEM() {


        runTestHandlingFlakyAndCompilation(
                "EnumEM",
                "org.foo.EnumEM",
                100
        ) { args: MutableList<String> ->

            //make sure enums are actually used, and not constraints solved with TT
            args.add("--baseTaintAnalysisProbability")
            args.add("0")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/enum", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/enum", "OK")
        }
    }
}