package org.evomaster.e2etests.spring.openapi.v3.wiremock.inet

import com.foo.rest.examples.spring.openapi.v3.wiremock.inet.InetReplacementController
import org.evomaster.ci.utils.CIUtils
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class InetReplacementEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(InetReplacementController(), config)
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "InetReplacementEM",
            "org.foo.InetReplacementEM",
            1000,
//            !CIUtils.isRunningGA(),
            false,
            { args: MutableList<String> ->

                args.add("--externalServiceIPSelectionStrategy")
                args.add("USER")
                args.add("--externalServiceIP")
                args.add("127.0.0.2")

                val solution = initAndRun(args)

                Assertions.assertTrue(solution.individuals.size >= 1)

                if (!CIUtils.isRunningGA()) {
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/inet/exp", "OK")
                }
            },
            3
        )
    }
}