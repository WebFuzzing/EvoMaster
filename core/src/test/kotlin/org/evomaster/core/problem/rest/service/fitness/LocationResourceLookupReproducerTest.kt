package org.evomaster.core.problem.rest.service.fitness

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import org.evomaster.client.java.controller.api.dto.TestResultsDto
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.problem.rest.service.ResourceManageService
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.action.ActionResult
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

/**
 * Run on unmodified 06ebbceb4ceb9c6585d7439ffd9bfda4cc75e7d5.
 * This regression test SHOULD FAIL there with:
 *   IllegalArgumentException: cannot find the resource with a key /
 *
 * HTTP execution/coverage transport is mocked to isolate the resource update.
 * doCalculateCoverage and ResourceManageService's lookup are real production code.
 * No local fix, dynamic-path flag, SUT, database or one-hour search is required.
 */
class LocationResourceLookupReproducerTest {
    @Test
    fun `Location follow-up must not be looked up as a schema root resource`() {
        // Same placeholder representation used by HttpSemanticsService.invalidLocation.
        val followUp = RestCallAction(
            id = "GET:LOCATION-FOLLOWUP",
            verb = HttpVerb.GET,
            path = RestPath("/"),
            parameters = mutableListOf(),
            usePreviousLocationId = "creator_0"
        )
        val individual = mockk<RestIndividual>(relaxed = true)
        every { individual.seeMainExecutableActions() } returns listOf(followUp)

        // A real resource manager: a declared non-root resource exists; '/' does not.
        val resources = ResourceManageService()
        resources.initExcludedResourceNode(listOf(RestCallAction(
            "POST:/items", HttpVerb.POST, RestPath("/items"), mutableListOf()
        )))

        val fitness = mockk<ResourceRestFitness>(relaxed = true)
        inject(fitness, "rm", resources)
        for (name in listOf("rc", "client", "harvestResponseHandler")) {
            val field = field(fitness, name)
            field.set(fitness, mockkClass(field.type.kotlin, relaxed = true))
        }
        every { fitness["doCalculateCoverage"](individual, emptySet<Int>(), false, false, false) } answers {
            callOriginal()
        }
        // Simulate a successfully retrieved result, so production code reaches resource maintenance.
        every {
            fitness["restActionResultHandling"](
                any<RestIndividual>(), any<Set<Int>>(), any<Boolean>(), any<Boolean>(),
                any<Boolean>(), any<List<ActionResult>>(), any<FitnessValue>()
            )
        } returns TestResultsDto()

        assertDoesNotThrow {
            val calculate = ResourceRestFitness::class.java.declaredMethods.single {
                it.name == "doCalculateCoverage" && it.parameterTypes.first() == RestIndividual::class.java
            }.apply { isAccessible = true }
            try {
                calculate.invoke(fitness, individual, emptySet<Int>(), false, false, false)
            } catch (e: java.lang.reflect.InvocationTargetException) {
                throw e.targetException
            }
        }
    }

    private fun field(target: Any, name: String): java.lang.reflect.Field {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            val f = type.declaredFields.firstOrNull { it.name == name }
            if (f != null) return f.apply { isAccessible = true }
            type = type.superclass
        }
        error("Missing field: $name")
    }

    private fun inject(target: Any, name: String, value: Any) = field(target, name).set(target, value)
}
