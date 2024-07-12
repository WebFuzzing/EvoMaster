package org.evomaster.e2etests.spring.rest.bb.authcookie

import com.foo.rest.examples.bb.authcookie.CookieLoginController
import com.foo.rest.examples.bb.authheader.BBAuthController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBAuthCookieEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(CookieLoginController())
        }
    }

    @ParameterizedTest
    @EnumSource(names = ["JS_JEST"]) //TODO add Python
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "authcookie",
            20,
            3,
            "OK"
        ){ args: MutableList<String> ->

        //TODO

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.POST, 200)
            assertNone(solution, HttpVerb.POST, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/logintoken/check", "this:foo")
        }
    }
}