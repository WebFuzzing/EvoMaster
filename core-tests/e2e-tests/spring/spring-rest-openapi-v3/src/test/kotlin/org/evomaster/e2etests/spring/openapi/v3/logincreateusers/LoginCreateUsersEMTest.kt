package org.evomaster.e2etests.spring.openapi.v3.logincreateusers

import com.foo.rest.examples.spring.openapi.v3.logincreateusers.LoginCreateUsersController
import com.foo.rest.examples.spring.openapi.v3.logintoken.LoginTokenController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class LoginCreateUsersEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(LoginCreateUsersController())
        }
    }


    @Test
    fun testRunBlackBoxEM() {
        runTestHandlingFlakyAndCompilation(
            "LoginCreateUsersEMBlackBox",
            50
        ) { args: MutableList<String> ->

            setOption(args, "blackBox", "true")
            setOption(args, "base", baseUrlOfSut)
            setOption(args, "schema", "$baseUrlOfSut/v3/api-docs")
            setOption(args, "configPath", "src/main/resources/config/logincreateusers.yaml")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/logincreateusers/check", "OK")
        }
    }

}