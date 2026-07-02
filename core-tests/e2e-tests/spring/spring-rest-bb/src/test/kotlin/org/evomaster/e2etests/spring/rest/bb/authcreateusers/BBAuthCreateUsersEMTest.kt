package org.evomaster.e2etests.spring.rest.bb.authcreateusers

import com.foo.rest.examples.bb.authcookie.CookieLoginController
import com.foo.rest.examples.bb.authcreateusers.AuthCreateUsersController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.evomaster.e2etests.utils.EnterpriseTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBAuthCreateUsersEMTest : SpringTestBase() {

    companion object {
        init {
            shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(AuthCreateUsersController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "authcreateusers",
            50,
            3,
            "CHECK"
        ){ args: MutableList<String> ->

            setOption(args, "configPath", "src/test/resources/config/authcreateusers.yaml")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/authcreateusers/check", "OK")
        }
    }
}