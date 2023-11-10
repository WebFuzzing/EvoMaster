package org.evomaster.e2etests.spring.openapi.v3.bbdefault

import com.foo.rest.examples.spring.openapi.v3.bbauth.BBAuthController
import com.foo.rest.examples.spring.openapi.v3.bbdefault.BBDefaultController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BBDefaultEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBDefaultController())
        }
    }


    @Test
    fun testRunEMOk() {
        runTestHandlingFlakyAndCompilation(
                "BBDefaultEMOk",
                "org.foo.BBDefaultEMOk",
                20
        ) { args: MutableList<String> ->

            args.add("--blackBox")
            args.add("true")
            args.add("--bbTargetUrl")
            args.add(baseUrlOfSut)
            args.add("--bbSwaggerUrl")
            args.add("$baseUrlOfSut/openapi-bbdefault.json")
            args.add("--bbExperiments")
            args.add("false")

            args.add("--probRestDefault")
            args.add("0.5")

            //make sure we do not solve it via taint analysis
            args.add("--baseTaintAnalysisProbability")
            args.add("0")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbdefault", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbdefault/{x}", "OK")
        }
    }

    @Test
    fun testRunEMFail() {
        runTestHandlingFlakyAndCompilation(
                "BBDefaultEMFail",
                "org.foo.BBDefaultEMFail",
                20
        ) { args: MutableList<String> ->

            args.add("--blackBox")
            args.add("true")
            args.add("--bbTargetUrl")
            args.add(baseUrlOfSut)
            args.add("--bbSwaggerUrl")
            args.add("$baseUrlOfSut/openapi-bbdefault.json")
            args.add("--bbExperiments")
            args.add("false")

            args.add("--probRestDefault")
            args.add("0.0") // no way in BB should be able to get the right string with no further info

            //make sure we do not solve it via taint analysis
            args.add("--baseTaintAnalysisProbability")
            args.add("0")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/bbdefault", null)
            assertNone(solution,HttpVerb.GET,200,"/api/bbdefault", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/bbdefault/{x}", null)
            assertNone(solution,HttpVerb.GET,200,"/api/bbdefault/{x}", "OK")
        }
    }

}