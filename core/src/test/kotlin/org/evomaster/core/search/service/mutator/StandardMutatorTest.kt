package org.evomaster.core.search.service.mutator

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.client.java.controller.api.dto.TestResultsDto
import org.evomaster.client.java.controller.api.dto.UnitsInfoDto
import org.evomaster.core.problem.api.param.UpdateForParam
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.param.AsyncApiParam
import org.evomaster.core.problem.asyncapi.param.UpdateForAsyncApiPayloadParam
import org.evomaster.core.problem.asyncapi.service.AsyncApiSampler
import org.evomaster.core.problem.asyncapi.service.AsyncApiTestInjector
import org.evomaster.core.problem.asyncapi.service.FakeAsyncApiDriver
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.time.ExecutionPhaseController
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StandardMutatorTest {

    companion object {

        private const val DTO = "com.foo.shop.ReturnRequested"

        private const val FIELD = "reason"

        /**
         * The schema of [DTO], keyed by its name the way the driver reports it.
         */
        private val DTO_SCHEMAS = mapOf(DTO to """"$DTO":{"type":"object","properties":{"$FIELD":{"type":"string"}}}""")

        /**
         * A message that declares no payload, but headers.
         */
        private val OPAQUE = """
            asyncapi: 3.0.0
            info:
              title: An opaque payload
              version: 1.0.0
            channels:
              returns:
                address: shop.returns
                messages:
                  returnRequested:
                    headers:
                      type: object
                      required: [tenant]
                      properties:
                        tenant:
                          type: string
            operations:
              requestReturn:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/returns'
        """.trimIndent()
    }

    private lateinit var injector: Injector

    @BeforeEach
    fun reset() {
        RestActionBuilderV3.cleanCache()
    }

    private fun start() {

        val driver = FakeAsyncApiDriver(AsyncApiTestInjector.sutInfo(OPAQUE)) { FakeAsyncApiDriver.fireAndForget() }
        driver.info.unitsInfoDto = UnitsInfoDto().apply { parsedDtos = DTO_SCHEMAS }
        driver.testResults = {
            TestResultsDto().apply { additionalInfoList.add(AdditionalInfoDto().apply { parsedDtoNames.add(DTO) }) }
        }

        //only gene mutation swaps an update in, so the structure is left alone
        injector = AsyncApiTestInjector.create(
            driver, "--blackBox=false", "--expandAsyncApiPayloads=true", "--enableStructureMutation=false",
            "--maxTestSize=1"
        )
        injector.getInstance(ExecutionPhaseController::class.java).markStartingSearch()
    }

    @Test
    fun testAPendingPayloadUpdateIsSwappedInBeforeMutating() {

        start()

        val sampler = injector.getInstance(AsyncApiSampler::class.java)
        val fitness = injector.getInstance(Key.get(object : TypeLiteral<FitnessFunction<AsyncApiIndividual>>() {}))
        val mutator = injector.getInstance(Key.get(object : TypeLiteral<Mutator<AsyncApiIndividual>>() {}))

        val evaluated = fitness.calculateCoverage(sampler.sample(forceRandomSample = true), modifiedSpec = null)
            ?: fail("the fitness gave up on the individual")
        assertTrue(evaluated.individual.seeMainExecutableActions().all { action ->
            (action as AsyncApiAction).parameters.any { it is UpdateForAsyncApiPayloadParam }
        })

        val mutated = mutator.mutate(evaluated)

        mutated.seeMainExecutableActions().forEach { action ->
            action as AsyncApiAction
            assertTrue(action.parameters.none { it is UpdateForParam }, "the update was left pending")

            val payloads = action.parameters.filter { it.name == AsyncApiParam.PAYLOAD }
            assertEquals(1, payloads.size, "the opaque payload was kept beside its replacement")
            val payload = payloads.single() as AsyncApiParam
            assertFalse(payload.opaque)
            assertEquals(listOf(FIELD), (payload.gene as ObjectGene).fields.map { it.name })

            //the headers are not what was replaced
            assertEquals(1, action.parameters.count { it.name == AsyncApiParam.HEADERS })
        }
    }
}
