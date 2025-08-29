package org.evomaster.e2etests.spring.openapi.v3.wiremock.socket

import com.foo.rest.examples.spring.openapi.v3.wiremock.socket.SocketController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SocketEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(SocketController(), config)
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "SocketEMTest",
            100
        ) { args: MutableList<String> ->

            args.add("--externalServiceIPSelectionStrategy")
            args.add("USER")
            args.add("--externalServiceIP")
            args.add("127.1.1.14")

            val solution = initAndRun(args)
            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/resolve", "OK")
            //This is quite different from HostnameResolutionActionEMTest
            assertNone(solution, HttpVerb.GET, 500)
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/resolve", null)
        }
    }
}
