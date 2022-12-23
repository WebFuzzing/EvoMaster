package org.evomaster.e2etests.spring.openapi.v3.wiremock.skip

import com.foo.rest.examples.spring.openapi.v3.wiremock.skip.SkipController
import org.evomaster.ci.utils.CIUtils
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class SkipEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(SkipController(), config)
        }
    }

    @Test
    fun testSkippingEM() {
        runTestHandlingFlakyAndCompilation(
            "SkipExternalServiceEM",
            "org.foo.SkipExternalServiceEM",
            1000,
            !CIUtils.isRunningGA(),
            { args: MutableList<String> ->

                args.add("--externalServiceIPSelectionStrategy")
                args.add("USER")
                args.add("--externalServiceIP")
                args.add("127.0.0.2")

                val solution = initAndRun(args)

                Assertions.assertTrue(solution.individuals.size >= 1)

                if (!CIUtils.isRunningGA()) {
//                    assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/skip", "OK")
                    assertNone(solution, HttpVerb.GET, 200)
                }
            },
            3
        )
    }
}