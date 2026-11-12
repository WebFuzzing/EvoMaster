package org.evomaster.e2etests.spring.openapi.v3.logintokensendheader

import com.foo.rest.examples.spring.openapi.v3.logintokensendheader.LoginTokenSendHeaderController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.evomaster.e2etests.utils.E2ETestConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class LoginTokenSendHeaderEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(LoginTokenSendHeaderController())
        }
    }

    @ParameterizedTest
    @EnumSource(names = ["KOTLIN_JUNIT_5", "JAVA_JUNIT_5"])
    fun testRunEM(outputFormat: OutputFormat) {
        runTestHandlingFlakyAndCompilation(
                E2ETestConfig.forLabel("LoginTokenSendHeaderEM_$outputFormat", 20)
                        .withOutputFormat(outputFormat)
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.POST, 200)
            assertNone(solution, HttpVerb.POST, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/logintokensendheader/check", "OK")
        }
    }

}