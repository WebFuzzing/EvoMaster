package org.evomaster.e2etests.spring.openapi.v3.wiremock.jsonarray

import com.foo.rest.examples.spring.openapi.v3.wiremock.jsonarray.WmJsonArrayController
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.evomaster.e2etests.utils.E2ETestConfig
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class WmJsonArrayEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(WmJsonArrayController(), config)
        }
    }

    @ParameterizedTest
    @EnumSource(names = ["KOTLIN_JUNIT_5", "JAVA_JUNIT_5"])
    fun testRunEM(outputFormat: OutputFormat) {

        runTestHandlingFlakyAndCompilation(
            E2ETestConfig("WmJsonArrayEM_$outputFormat", "org.foo.WmJsonArrayEM_$outputFormat", 100)
                .withOutputFormat(outputFormat)
        ) { args: MutableList<String> ->
            args.add("--externalServiceIPSelectionStrategy")
            args.add("USER")
            args.add("--externalServiceIP")
            args.add("127.0.0.26")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wm/jsonarray", "OK X")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wm/jsonarray", "OK X and Y")
        }
    }

}
