package org.evomaster.e2etests.spring.openapi.v3.restindividualbuilder

import com.foo.rest.examples.spring.openapi.v3.restindividualbuilder.RestIndividiualBuilderController
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class RestIndividualBuilderEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(RestIndividiualBuilderController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "RestIndividualBuilderEM",
            1000
        ) { args: MutableList<String> ->

            args.add("--blackBox")
            args.add("true")
            args.add("--bbTargetUrl")
            args.add(baseUrlOfSut)
            args.add("--bbSwaggerUrl")
            args.add("$baseUrlOfSut/v3/api-docs")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
        }
    }
}