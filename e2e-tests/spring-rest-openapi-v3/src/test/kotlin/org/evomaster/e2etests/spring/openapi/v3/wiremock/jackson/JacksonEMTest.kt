package org.evomaster.e2etests.spring.openapi.v3.wiremock.jackson

import com.foo.rest.examples.spring.openapi.v3.wiremock.jackson.JacksonController
import org.evomaster.ci.utils.CIUtils
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class JacksonEMTest {

    companion object : SpringTestBase() {
        @BeforeAll
        @JvmStatic
        fun init() {

            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(JacksonController(), config)

            /*
            The test fails on CI, but not local with WM 2.32.0
            if updating WM to 2.34.0, the test fails on local windows as well (TO CHECK)
            */
            CIUtils.skipIfOnGA()
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "WmAuth0EM",
            "org.foo.WmAuth0EM",
            500,
            false,
            { args: MutableList<String> ->

                args.add("--externalServiceIPSelectionStrategy")
                args.add("USER")
                args.add("--externalServiceIP")
                args.add("127.0.0.2")
                args.add("--instrumentMR_NET")
                args.add("true")

                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/jackson/wm", "OK")
            },
            3
        )
    }
}