package org.evomaster.e2etests.spring.rest.bb.flakinessdetect


import com.foo.rest.examples.bb.flakinessdetect.FlakinessDetectController
import org.evomaster.core.output.OutputFormat
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class FlakinessDetectBBEMTest : SpringTestBase() {

    companion object {
        init {
            shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(FlakinessDetectController())
        }
    }


    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {
        executeAndEvaluateBBTest(
            outputFormat,
            "flakinessdetect",
            100,
            3,
            listOf("TimeAgo", "estimate","MultipleLines","Next","First")
        ){ args: MutableList<String> ->

            setOption(args, "handleFlakiness", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())

        }
    }
}