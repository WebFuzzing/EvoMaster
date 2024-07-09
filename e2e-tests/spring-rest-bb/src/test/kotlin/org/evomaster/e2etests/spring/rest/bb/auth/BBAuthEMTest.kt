package org.evomaster.e2etests.spring.rest.bb.auth

import com.foo.rest.examples.bb.auth.BBAuthController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
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
    fun testJavaScript(){

       // runTestHandlingFlaky()
        //TODO
    }


    @Test
    fun testRunEMOk() {
        runTestHandlingFlakyAndCompilation(
                "BBAuthEMOk",
                "org.foo.BBAuthEMOk",
                20
        ) { args: MutableList<String> ->

            addBlackBoxOptions(args, OutputFormat.KOTLIN_JUNIT_5)
            setOption(args, "header0", "X-FOO:foo")
            setOption(args, "header1", "X-BAR:42")
            setOption(args, "header2", "Authorization:token")

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

            addBlackBoxOptions(args, OutputFormat.KOTLIN_JUNIT_5)
            setOption(args, "configPath", "src/main/resources/config/bbauth.toml")

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

            addBlackBoxOptions(args, OutputFormat.KOTLIN_JUNIT_5)
            //setOption(args, "header0", "X-FOO:foo")
            setOption(args, "header1", "X-BAR:42")
            setOption(args, "header2", "Authorization:token")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/bbauth", null)
        }
    }

}