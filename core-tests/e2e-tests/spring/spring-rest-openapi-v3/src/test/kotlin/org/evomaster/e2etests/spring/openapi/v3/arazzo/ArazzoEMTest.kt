package org.evomaster.e2etests.spring.openapi.v3.arazzo

import com.foo.rest.examples.spring.openapi.v3.arazzo.ArazzoPetCouponsController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.service.ArazzoWorkflowsService
import org.evomaster.core.problem.rest.service.sampler.RestSampler
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ArazzoEMTest : SpringTestBase() {

    companion object {
        private const val ARAZZO_LOCATION = "src/main/resources/static/pet-coupons-arazzo.yaml"

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ArazzoPetCouponsController())
        }
    }

    @Test
    fun testRunEMWithArazzoWorkflowSampling() {
        runTestHandlingFlakyAndCompilation(
            "ArazzoEM",
            "org.foo.ArazzoEM",
            50,
        ) { args ->
            setOption(args, "enableArazzoWorkflowSampling", "true")
            setOption(args, "arazzoLocation", ARAZZO_LOCATION)
            setOption(args, "probOfArazzoSampling", "1.0")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
        }
    }

    @Test
    fun testArazzoWorkflowsMatchPetCouponsSpec() {
        runTestHandlingFlaky(
            "ArazzoWorkflowShape",
            "org.foo.ArazzoWorkflowShape",
            1,
            false,
        ) { args ->
            setOption(args, "enableArazzoWorkflowSampling", "true")
            setOption(args, "arazzoLocation", ARAZZO_LOCATION)
            setOption(args, "probOfArazzoSampling", "1.0")

            val injector = init(args)
            val arazzoService = injector.getInstance(ArazzoWorkflowsService::class.java)
            val sampler = injector.getInstance(RestSampler::class.java)

            assertTrue(arazzoService.arazzoWorkflows.isNotEmpty())
            assertTrue(sampler.numberOfDistinctActions() > 0)

            val workflow = arazzoService.arazzoWorkflowsById["apply-coupon"]!!
            val ind = arazzoService.buildIndividualFromWorkflow(workflow)
            val actions = ind.seeAllActions().filterIsInstance<RestCallAction>()

            assertEquals(
                listOf("findPetsByTags", "getPetCoupons", "placeOrder"),
                actions.map { it.operationId },
            )
            assertEquals(
                listOf(HttpVerb.GET, HttpVerb.GET, HttpVerb.POST),
                actions.map { it.verb },
            )
            assertEquals(
                listOf("/pet/findByTags", "/pet/{petId}/coupons", "/store/order"),
                actions.map { it.path.toString() },
            )
        }
    }
}
