package org.evomaster.e2etests.spring.openapi.v3.wiremock.socketconnect

import com.foo.rest.examples.spring.openapi.v3.wiremock.socketconnect.WmSocketConnectController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 *
 */

class WmSocketConnectEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(WmSocketConnectController(), config)
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "WmSocketConnectEM",
            "org.foo.WmSocketConnectOpenEM",
            500,
            false,
            { args: MutableList<String> ->

                args.add("--externalServiceIPSelectionStrategy")
                args.add("USER")
                args.add("--externalServiceIP")
                args.add("127.0.0.12")

                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wm/socketconnect/string", "OK")
//                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wm/socketconnect/string", "Hello There")
                assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/wm/socketconnect/string", null)
                assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/wm/socketconnect/string", null)
            },
            3
        )
    }

}