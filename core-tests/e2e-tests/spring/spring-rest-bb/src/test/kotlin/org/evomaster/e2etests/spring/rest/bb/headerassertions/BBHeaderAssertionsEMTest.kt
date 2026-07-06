package org.evomaster.e2etests.spring.rest.bb.headerassertions

import com.foo.rest.examples.bb.headerassertions.BBHeaderAssertionsController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBHeaderAssertionsEMTest : SpringTestBase() {

    companion object {
        init {
            shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBHeaderAssertionsController())
        }
    }



    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "headerassertions",
            100,
            3,
            listOf("ok401","fail401","ok405","fail405","ok426","fail426")
        ){ args: MutableList<String> ->

            setOption(args, "useExperimentalOracles", "true")
            setOption(args, "statusOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val prefix = "/api/headerassertions"

            assertHasAtLeastOne(solution, HttpVerb.GET, 401, "$prefix/ok/401", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 401, "$prefix/fail/401", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 405, "$prefix/ok/405", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 405, "$prefix/fail/405", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 426, "$prefix/ok/426", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 426, "$prefix/fail/426", null)

            val faults = DetectedFaultUtils.getDetectedFaults(solution)
            assertTrue(faults.isNotEmpty())

            assertTrue(faults.none{
                it.operationId == "GET:$prefix/ok/401"
                        && it.category == ExperimentalFaultCategory.HTTP_STATUS_NO_401_IF_NO_WWW_AUTHENTICATE })
            assertTrue(faults.any{
                it.operationId == "GET:$prefix/fail/401"
                        && it.category == ExperimentalFaultCategory.HTTP_STATUS_NO_401_IF_NO_WWW_AUTHENTICATE })

            assertTrue(faults.none{
                it.operationId == "GET:$prefix/ok/405"
                        && it.category == ExperimentalFaultCategory.HTTP_STATUS_NO_405_IF_NO_ALLOW })
            assertTrue(faults.any{
                it.operationId == "GET:$prefix/fail/405"
                        && it.category == ExperimentalFaultCategory.HTTP_STATUS_NO_405_IF_NO_ALLOW })

            assertTrue(faults.none{
                it.operationId == "GET:$prefix/ok/426"
                        && it.category == ExperimentalFaultCategory.HTTP_STATUS_NO_426_IF_NO_UPGRADE })
            assertTrue(faults.any{
                it.operationId == "GET:$prefix/fail/426"
                        && it.category == ExperimentalFaultCategory.HTTP_STATUS_NO_426_IF_NO_UPGRADE })
        }
    }
}
