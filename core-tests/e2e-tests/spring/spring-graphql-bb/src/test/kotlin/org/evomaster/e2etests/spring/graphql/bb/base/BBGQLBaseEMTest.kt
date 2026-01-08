package org.evomaster.e2etests.spring.graphql.bb.base

import com.foo.graphql.bb.base.BaseController
import org.evomaster.core.output.OutputFormat
import org.evomaster.e2etests.spring.graphql.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBGQLBaseEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BaseController())
        }
    }


    @ParameterizedTest
    @EnumSource(names = ["JS_JEST", "PYTHON_UNITTEST"])
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "base",
            30,
            3,
            listOf("ALL")
        ){ args: MutableList<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOneResponseWithData(solution)
            assertValueInDataAtLeastOnce(solution, "Foo")
            assertNoneWithErrors(solution)
        }
    }

}
