package org.evomaster.e2etests.spring.rest.bb.authcookie

import com.foo.rest.examples.bb.authcookie.CookieLoginController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
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
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "authcookie",
            20,
            3,
            "FOO"
        ){ args: MutableList<String> ->

            setOption(args, "configPath", "src/test/resources/config/authcookie.yaml")
            setOption(args, "endpointFocus", "/api/logintoken/check")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.POST, 200)
            assertNone(solution, HttpVerb.POST, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/logintoken/check", "this:foo")
        }
    }
}