package org.evomaster.core.output.naming

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCExceptionInfoDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.exception.RPCExceptionType
import org.evomaster.core.output.EvaluatedIndividualBuilder
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.rpc.RPCCallResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class RPCActionNamingStrategyTest {

    companion object {
        val outputFormat = OutputFormat.KOTLIN_JUNIT_5
        val languageConventionFormatter = LanguageConventionFormatter(outputFormat)
        const val MAX_NAME_LENGTH = 80
    }

    @Test
    fun testFakeRpcCallAsInterfaceIdAndId() {
        val solution = getSolution(outputFormat)

        val namingStrategy = RPCActionTestCaseNamingStrategy(solution, languageConventionFormatter, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_fakeRPCCallOnFunction_4ReturnsSuccess", testCases[0].name)
    }

    @Test
    fun testFakeRpcCallWithException() {
        val solution = getSolution(outputFormat, true)

        val namingStrategy = RPCActionTestCaseNamingStrategy(solution, languageConventionFormatter, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_fakeRPCCallOnFunction_4ThrowsRuntimeException", testCases[0].name)
    }

    @Test
    fun testClassAndFunctionNamesAreAddedIfAllowedByLength() {
        val solution = getSolution(outputFormat, true)

        val withClassAndFunctionName = RPCActionTestCaseNamingStrategy(solution, languageConventionFormatter, 30).getTestCases()
        val noClassAndFunctionName = RPCActionTestCaseNamingStrategy(solution, languageConventionFormatter, 15).getTestCases()

        assertEquals(1, withClassAndFunctionName.size)
        assertEquals(1, noClassAndFunctionName.size)
        assertEquals("test_0_fakeRPCCallOnFunction_4", withClassAndFunctionName[0].name)
        assertEquals("test_0", noClassAndFunctionName[0].name)
    }

    private fun getSolution(outputFormat: OutputFormat, throwsException: Boolean = false): Solution<*> {
        return Solution(
            mutableListOf(getEvaluatedIndividual(outputFormat, throwsException)),
            "suitePrefix",
            "suiteSuffix",
            Termination.NONE,
            listOf(),
            listOf()
        )
    }

    private fun getEvaluatedIndividual(outputFormat: OutputFormat, throwsException: Boolean): EvaluatedIndividual<*> {
        val expectedJson = 5

        //build fake rpc individual in order to test its generated tests
        val evaluatedIndividual = EvaluatedIndividualBuilder.buildEvaluatedRPCIndividual(
            actions = EvaluatedIndividualBuilder.buildFakeRPCAction(expectedJson, "FakeRPCCall:function"),
            externalServicesActions = (0 until expectedJson).map {
                EvaluatedIndividualBuilder.buildFakeDbExternalServiceAction(1).plus(EvaluatedIndividualBuilder.buildFakeRPCExternalServiceAction(1))
            }.toMutableList(),

            format = outputFormat
        )
        if (throwsException) {
            (evaluatedIndividual.evaluatedMainActions().last().result as RPCCallResult).setRPCException(getExceptionDto())
        }
        return evaluatedIndividual
    }

    private fun getExceptionDto(): RPCExceptionInfoDto {
        val eDto = RPCExceptionInfoDto()
        eDto.exceptionName = "java.lang.RuntimeException"
        eDto.type = RPCExceptionType.UNEXPECTED_EXCEPTION
        eDto.importanceLevel = 0
        eDto.isCauseOfUndeclaredThrowable = true
        return eDto
    }
}
