package org.evomaster.core.problem.rest.service.sampler

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.swagger.v3.parser.OpenAPIV3Parser
import org.evomaster.arazzo.access.ArazzoAccess
import org.evomaster.arazzo.models.domain.Workflow
import org.evomaster.arazzo.parser.ArazzoParser
import org.evomaster.core.EMConfig
import org.evomaster.core.Main
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.SearchGlobalState
import org.evomaster.core.search.service.time.SearchTimeController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class ArazzoSamplerTest {

    companion object {
        private lateinit var sampler: ArazzoSampler

        init {
            Main.applyGlobalJVMSettings()
        }

        @BeforeAll
        @JvmStatic
        fun setUp() {
            val openApiPath = resourcePath("/openapi/openapi_pet.json")
            val arazzoPath = resourcePath("/arazzo/arazzo_pet.yaml")
            val config = EMConfig()
            val openAPI = OpenAPIV3Parser().read(openApiPath)
            val allWorkflows = ArazzoParser.parse(ArazzoAccess.readFromDisk(arazzoPath), openAPI).workflows

            val actions = mutableMapOf<String, Action>()
            RestActionBuilderV3.addActionsFromSwagger(
                RestSchema.fromLocation(openApiPath),
                actions,
                options = RestActionBuilderV3.Options(config),
            )

            val workflow = allWorkflows.first { it.workflowId == "buy-available-pet" }

            sampler = ArazzoSampler()
            wireSampler(
                sampler = sampler,
                config = config,
                workflows = listOf(workflow),
                workflowsById = allWorkflows.associateBy { it.workflowId },
                actions = actions,
            )
        }

        private fun resourcePath(resource: String): String {
            val url = ArazzoSamplerTest::class.java.getResource(resource)
                ?: throw IllegalStateException("Missing test resource: $resource")
            return Paths.get(url.toURI()).toString()
        }

        /**
         * Injects mocked infrastructure into the sampler.
         * Reflection is still required because [Sampler] fields are @Inject protected.
         */
        private fun wireSampler(
            sampler: ArazzoSampler,
            config: EMConfig,
            workflows: List<Workflow>,
            workflowsById: Map<String, Workflow>,
            actions: Map<String, Action>,
        ) {
            val time = mockk<SearchTimeController>(relaxed = true)
            every { time.percentageUsedBudget() } returns 0.0
            every { time.evaluatedIndividuals } returns 0

            val apc = mockk<AdaptiveParameterControl>(relaxed = true)

            // spyk: real gene initialization for RestCallActions, mocked choose() for determinism
            val randomness = spyk(Randomness().also {
                it.setField(Randomness::class.java, "configuration", config)
                it.updateSeed(42)
            })
            every { randomness.choose(workflows) } returns workflows.first()

            val searchGlobalState = mockk<SearchGlobalState>(relaxed = true)
            every { searchGlobalState.randomness } returns randomness
            every { searchGlobalState.config } returns config
            every { searchGlobalState.time } returns time
            every { searchGlobalState.apc } returns apc

            sampler.setField(Sampler::class.java, "randomness", randomness)
            sampler.setField(Sampler::class.java, "config", config)
            sampler.setField(Sampler::class.java, "time", time)
            sampler.setField(Sampler::class.java, "apc", apc)
            sampler.setField(Sampler::class.java, "searchGlobalState", searchGlobalState)

            sampler.workflowsArazzo.clear()
            sampler.workflowsArazzo.addAll(workflows)
            sampler.setField(AbstractRestSampler::class.java, "workflowsArazzoById", workflowsById)

            val cluster = sampler.getField<MutableMap<String, Action>>(Sampler::class.java, "actionCluster")
            cluster.clear()
            cluster.putAll(actions)
        }

        private fun Any.setField(clazz: Class<*>, name: String, value: Any?) {
            val field = clazz.getDeclaredField(name)
            field.isAccessible = true
            field.set(this, value)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> Any.getField(clazz: Class<*>, name: String): T {
            val field = clazz.getDeclaredField(name)
            field.isAccessible = true
            return field.get(this) as T
        }
    }

    @Test
    fun sampleAtRandomBuildsIndividualFromWorkflowSteps() {
        // sampleAtRandom() is protected; forceRandomSample invokes it directly
        val individual = sampler.sample(forceRandomSample = true)

        val operationIds = individual.seeMainExecutableActions()
            .filterIsInstance<RestCallAction>()
            .map { it.operationId }

        // buy-available-pet: findPetsByStatus -> nested place-order -> placeOrder
        assertEquals(listOf("findPetsByStatus", "placeOrder"), operationIds)
    }
}
