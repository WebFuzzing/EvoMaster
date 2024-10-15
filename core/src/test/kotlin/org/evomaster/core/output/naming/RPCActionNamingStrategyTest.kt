package org.evomaster.core.output.naming

import org.evomaster.core.output.EvaluatedIndividualBuilder
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.search.Solution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class RPCActionNamingStrategyTest {

    @Test
    fun testFakeRpcCallAsInterfaceIdAndId() {
        val outputFormat = OutputFormat.KOTLIN_JUNIT_5
        val languageConventionFormatter = LanguageConventionFormatter(outputFormat)
        val solution = getSolution(outputFormat)

        val namingStrategy = RPCActionTestCaseNamingStrategy(solution, languageConventionFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_fakerpccallOnFunction_4ReturnsSuccess", testCases[0].name)
    }

    private fun getSolution(outputFormat: OutputFormat): Solution<*> {
        val expectedJson = 5

        return Solution(
            mutableListOf(
                //build fake rpc individual in order to test its generated tests
                EvaluatedIndividualBuilder.buildEvaluatedRPCIndividual(
                    actions = EvaluatedIndividualBuilder.buildFakeRPCAction(expectedJson, "FakeRPCCall:function"),
                    externalServicesActions = (0 until expectedJson).map {
                        EvaluatedIndividualBuilder.buildFakeDbExternalServiceAction(1).plus(EvaluatedIndividualBuilder.buildFakeRPCExternalServiceAction(1))
                    }.toMutableList(),

                    format = outputFormat
                )
            ),
            "suitePrefix",
            "suiteSuffix",
            Termination.NONE,
            listOf(),
            listOf()
        )
    }
}
