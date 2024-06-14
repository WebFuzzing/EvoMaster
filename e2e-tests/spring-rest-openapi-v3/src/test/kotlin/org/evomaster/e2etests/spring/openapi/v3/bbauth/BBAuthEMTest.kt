package org.evomaster.e2etests.spring.openapi.v3.bbauth

import com.foo.rest.examples.spring.openapi.v3.bbauth.BBAuthController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BBAuthEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBAuthController())
        }
    }


    @Test
    fun testRunEMOk() {
        runTestHandlingFlakyAndCompilation(
                "BBAuthEMOk",
                "org.foo.BBAuthEMOk",
                20
        ) { args: MutableList<String> ->

            args.add("--blackBox")
            args.add("true")
            args.add("--bbTargetUrl")
            args.add(baseUrlOfSut)
            args.add("--bbSwaggerUrl")
            args.add("$baseUrlOfSut/v3/api-docs")
            args.add("--bbExperiments")
            args.add("false")

            args.add("--header0")
            args.add("X-FOO:foo")
            args.add("--header1")
            args.add("X-BAR:42")
            args.add("--header2")
            args.add("Authorization:token")

            //make sure we do not solve it via taint analysis
            args.add("--baseTaintAnalysisProbability")
            args.add("0")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbauth", "OK")
        }
    }

    @Test
    fun testRunEMOkInConfig() {
        runTestHandlingFlakyAndCompilation(
            "BBAuthEMInConfig",
            "org.foo.BBAuthEMInConfig",
            20
        ) { args: MutableList<String> ->

            args.add("--blackBox")
            args.add("true")
            args.add("--bbTargetUrl")
            args.add(baseUrlOfSut)
            args.add("--bbSwaggerUrl")
            args.add("$baseUrlOfSut/v3/api-docs")
            args.add("--bbExperiments")
            args.add("false")

            args.add("--configPath")
            args.add("src/main/resources/config/bbauth.toml")

            //make sure we do not solve it via taint analysis
            args.add("--baseTaintAnalysisProbability")
            args.add("0")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbauth", "OK")
        }
    }

    @Test
    fun testRunEMFail() {
        runTestHandlingFlakyAndCompilation(
                "BBAuthEMFail",
                "org.foo.BBAuthEMFail",
                20
        ) { args: MutableList<String> ->

            args.add("--blackBox")
            args.add("true")
            args.add("--bbTargetUrl")
            args.add(baseUrlOfSut)
            args.add("--bbSwaggerUrl")
            args.add("$baseUrlOfSut/v3/api-docs")
            args.add("--bbExperiments")
            args.add("false")

//            args.add("--header0")
//            args.add("X-FOO:foo")
            args.add("--header1")
            args.add("X-BAR:42")
            args.add("--header2")
            args.add("Authorization:token")

            //make sure we do not solve it via taint analysis
            args.add("--baseTaintAnalysisProbability")
            args.add("0")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/bbauth", null)
        }
    }

}