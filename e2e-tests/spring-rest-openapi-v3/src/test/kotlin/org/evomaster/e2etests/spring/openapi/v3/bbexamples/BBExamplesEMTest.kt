package org.evomaster.e2etests.spring.openapi.v3.bbexamples


import com.foo.rest.examples.spring.openapi.v3.bbexamples.BBExamplesController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BBExamplesEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBExamplesController())
        }
    }


    @Test
    fun testRunEMOk() {
        runTestHandlingFlakyAndCompilation(
                "BBExamplesEMOk",
                "org.foo.BBExamplesEMOk",
                500
        ) { args: MutableList<String> ->

            args.add("--blackBox")
            args.add("true")
            args.add("--bbTargetUrl")
            args.add(baseUrlOfSut)
            args.add("--bbSwaggerUrl")
            args.add("$baseUrlOfSut/openapi-bbexamples.json")
            args.add("--bbExperiments")
            args.add("false")

            args.add("--probRestDefault")
            args.add("0.45")
            args.add("--probRestExamples")
            args.add("0.45")

            //make sure we do not solve it via taint analysis
            args.add("--baseTaintAnalysisProbability")
            args.add("0")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbexamples", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbexamples/{x}", "OK")
            //NOTE: as "default" is only applicable for optional params, and path params are always required, parser might skip it...
//            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbexamples/{x}/mixed", "12345")
            assertHasAtLeastOne(solution, HttpVerb.GET, 201, "/api/bbexamples/{x}/mixed", "456789")
            assertHasAtLeastOne(solution, HttpVerb.GET, 202, "/api/bbexamples/{x}/mixed", "778899")
            assertHasAtLeastOne(solution, HttpVerb.GET, 203, "/api/bbexamples/{x}/mixed", "Foo")
            assertHasAtLeastOne(solution, HttpVerb.GET, 250, "/api/bbexamples/{x}/mixed", "Bar")
            assertHasAtLeastOne(solution, HttpVerb.GET, 251, "/api/bbexamples/{x}/mixed", "Hello")
        }
    }

    @Test
    fun testRunEMFail() {
        runTestHandlingFlakyAndCompilation(
                "BBExamplesEMFail",
                "org.foo.BBExamplesEMFail",
                50
        ) { args: MutableList<String> ->

            args.add("--blackBox")
            args.add("true")
            args.add("--bbTargetUrl")
            args.add(baseUrlOfSut)
            args.add("--bbSwaggerUrl")
            args.add("$baseUrlOfSut/openapi-bbexamples.json")
            args.add("--bbExperiments")
            args.add("false")

            args.add("--probRestExamples")
            args.add("0.0")

            //make sure we do not solve it via taint analysis
            args.add("--baseTaintAnalysisProbability")
            args.add("0")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/bbexamples", null)
            assertNone(solution,HttpVerb.GET,200,"/api/bbexamples", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/bbexamples/{x}", null)
            assertNone(solution,HttpVerb.GET,200,"/api/bbexamples/{x}", "OK")
        }
    }

}