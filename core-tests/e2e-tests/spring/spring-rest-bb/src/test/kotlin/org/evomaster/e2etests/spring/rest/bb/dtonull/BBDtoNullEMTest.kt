package org.evomaster.e2etests.spring.rest.bb.dtonull

import com.foo.rest.examples.bb.dtonull.BBDtoNullController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBDtoNullEMTest : SpringTestBase() {

    companion object {
        init {
            shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBDtoNullController())
        }
    }



    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "dtonull",
            100,
            3,
            listOf("UNDEFINED", "NULL","POSITIVE","NEGATIVE")
        ){ args: MutableList<String> ->

            setOption(args, "dtoForRequestPayload", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbdtonull/items", "UNDEFINED")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbdtonull/items", "NULL")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbdtonull/items", "POSITIVE")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbdtonull/items", "NEGATIVE")
        }
    }
}
