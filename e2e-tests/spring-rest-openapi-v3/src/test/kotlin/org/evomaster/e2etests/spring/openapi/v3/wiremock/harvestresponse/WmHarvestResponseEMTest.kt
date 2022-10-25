package org.evomaster.e2etests.spring.openapi.v3.wiremock.harvestresponse

import com.foo.rest.examples.spring.openapi.v3.wiremock.harvestresponse.WmHarvestResponseController
import org.evomaster.ci.utils.CIUtils
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


class WmHarvestResponseEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {

            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(WmHarvestResponseController(), config)


            CIUtils.skipIfOnGA()
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "WmHarvestResponseEM",
            "org.foo.WmHarvestResponseEM",
            500,
            false,
            { args: MutableList<String> ->

                args.add("--externalServiceIPSelectionStrategy")
                args.add("USER")
                args.add("--externalServiceIP")
                args.add("127.0.0.22")
                args.add("--probOfHarvestingResponsesFromActualExternalServices")
                args.add("0.5")

                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wm/harvestresponse/norway", "OK")
            },
            3
        )
    }

}