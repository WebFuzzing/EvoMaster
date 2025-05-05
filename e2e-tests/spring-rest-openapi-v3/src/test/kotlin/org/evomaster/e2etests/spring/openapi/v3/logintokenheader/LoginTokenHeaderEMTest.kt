package org.evomaster.e2etests.spring.openapi.v3.logintokenheader

import com.foo.rest.examples.spring.openapi.v3.logintoken.LoginTokenController
import com.foo.rest.examples.spring.openapi.v3.logintokenheader.LoginTokenHeaderController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class LoginTokenHeaderEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(LoginTokenHeaderController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "LoginTokenHeaderEM",
                20
        ) { args: List<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.POST, 200)
            assertNone(solution, HttpVerb.POST, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/logintokenheader/check", "OK")
        }
    }

    @Test
    fun testRunBlackBoxEM() {
        runTestHandlingFlakyAndCompilation(
            "LoginTokenHeaderEMBlackBox",
            20
        ) { args: MutableList<String> ->

            setOption(args, "blackBox", "true")
            setOption(args, "bbTargetUrl", baseUrlOfSut)
            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/v3/api-docs")
            setOption(args, "endpointFocus", "/api/logintokenheader/check")
            setOption(args, "configPath", "src/main/resources/config/logintokenheader.yaml")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.POST, 200)
            assertNone(solution, HttpVerb.POST, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/logintokenheader/check", "OK")
        }
    }

}