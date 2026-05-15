package org.evomaster.core.problem.rest.service.fitness

import io.mockk.every
import io.mockk.mockk
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.TestUtils
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.IdMapper
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

internal class AbstractRestFitnessTest {

    private fun makeIndividual(numActions: Int): RestIndividual {
        val calls = (1..numActions).map { i ->
            RestResourceCalls(
                actions = listOf(TestUtils.generateFakeQueryRestAction("$i", "/path$i")),
                sqlActions = emptyList()
            )
        }.toMutableList()
        val ind = RestIndividual(calls, SampleType.RANDOM)
        ind.resetLocalIdRecursively()
        ind.doInitializeLocalId()
        ind.doInitialize()
        return ind
    }

    private fun injectField(target: Any, declaringClass: Class<*>, fieldName: String, value: Any) {
        val field = declaringClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    /**
     * Regression test for crash: Index 1 out of bounds for length 1 in expandIndividual
     * when actionResults is shorter than additionalInfoList.
     */
    @Test
    fun `expandIndividual does not crash when actionResults is shorter than additionalInfoList`() {
        val fitness = mockk<AbstractRestFitness>(relaxed = true)
        every { fitness.expandIndividual(any(), any(), any()) } answers { callOriginal() }

        val individual = makeIndividual(2)
        val result1 = RestCallResult("r1", false)

        assertDoesNotThrow {
            fitness.expandIndividual(
                individual,
                additionalInfoList = listOf(AdditionalInfoDto(), AdditionalInfoDto()),
                actionResults = listOf(result1)
            )
        }
    }

    /**
     * Regression test for crash: Index 1 out of bounds for length 1 in getlocation5xx
     * when handleResponseTargets iterates over actionResults but additionalInfoList is shorter.
     */
    @Test
    fun `handleResponseTargets does not crash when actionResults is longer than additionalInfoList with 500 status`() {
        val fitness = mockk<AbstractRestFitness>(relaxed = true)
        every { fitness.handleResponseTargets(any(), any(), any(), any()) } answers { callOriginal() }

        val idMapper = mockk<IdMapper>(relaxed = true)
        val config = EMConfig().also {
            it.security = false
            it.advancedBlackBoxCoverage = false
        }
        injectField(fitness, FitnessFunction::class.java, "idMapper", idMapper)
        injectField(fitness, FitnessFunction::class.java, "config", config)

        val action1 = TestUtils.generateFakeQueryRestAction("1", "/foo")
        val action2 = TestUtils.generateFakeQueryRestAction("2", "/bar")

        val result1 = RestCallResult("r1", false).also { it.setStatusCode(500) }
        val result2 = RestCallResult("r2", false).also { it.setStatusCode(500) }

        assertDoesNotThrow {
            fitness.handleResponseTargets(
                fv = FitnessValue(2.0),
                actions = listOf(action1, action2),
                actionResults = listOf(result1, result2),
                additionalInfoList = listOf(AdditionalInfoDto())
            )
        }
    }
}
