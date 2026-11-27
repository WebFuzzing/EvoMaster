package org.evomaster.core.problem.rest.arazzo

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.AbstractModule
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.client.java.controller.api.dto.ControllerInfoDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.problem.RestProblemDto
import org.evomaster.core.BaseModule
import org.evomaster.core.Main
import org.evomaster.core.ServiceStarter
import org.evomaster.core.problem.external.service.DummyController
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.service.ArazzoWorkflowsService
import org.evomaster.core.problem.rest.service.module.RestModule
import org.evomaster.core.remote.service.RemoteController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import javax.ws.rs.core.MediaType

class ArazzoRuntimeTest {

    companion object {
        init {
            Main.applyGlobalJVMSettings()
        }

        private const val ARAZZO = "src/test/resources/arazzo/arazzo-runtime-test.yaml"
        private const val OPENAPI = "src/test/resources/openapi/pet-coupons-openapi.yaml"
    }

    private lateinit var arazzoService: ArazzoWorkflowsService

    @BeforeEach
    fun init() {
        RestActionBuilderV3.cleanCache()
        arazzoService = createArazzoService()
    }

    @Test
    fun testStepOutputFeedsNextStepParameter() {
        val actions = actionsOf("find-pet-then-coupon")
        assertEquals(listOf("findPetsByTags", "getPetCoupons"), actions.map { it.operationId })

        execute(actions, ok("""[{"id": 123}]"""), ok("""{"couponCode": "SUMMER10"}"""))

        assertEquals("123", pathParam(actions[1], "petId"))
    }

    @Test
    fun testMissingOutputKeepsTheGeneratedValue() {
        val actions = actionsOf("find-pet-then-coupon")
        val generated = "777"
        assertTrue(actions[1].parameters.filterIsInstance<PathParam>().single().primaryGene().setFromStringValue(generated))

        //the first step fails, so there is nothing to read the id from
        execute(actions, notFound(), notFound())

        assertEquals(generated, pathParam(actions[1], "petId"))
    }

    @Test
    fun testNestedWorkflowReceivesInputsFromTheParentStep() {
        val actions = actionsOf("apply-coupon-and-check")
        assertEquals(listOf("findPetsByTags", "getPetCoupons", "placeOrder", "getOrderById"), actions.map { it.operationId })

        execute(
            actions,
            ok("""[{"id": 123}]"""),
            ok("""{"couponCode": "SUMMER10"}"""),
            ok("""{"id": 777}"""),
            ok("""{}""")
        )

        val body = body(actions[2])
        assertEquals("123", body.get("petId").asText())
        assertEquals("SUMMER10", body.get("couponCode").asText())
    }

    @Test
    fun testNestedWorkflowOutputReachesTheParentWorkflow() {
        val actions = actionsOf("apply-coupon-and-check")

        execute(
            actions,
            ok("""[{"id": 123}]"""),
            ok("""{"couponCode": "SUMMER10"}"""),
            ok("""{"id": 777}"""),
            ok("""{}""")
        )

        //the id comes from the response of placeOrder, going through the outputs of place-order
        assertEquals("777", pathParam(actions[3], "orderId"))
    }

    @Test
    fun testEachInvocationOfTheSameWorkflowHasItsOwnInputs() {
        val actions = actionsOf("double-order")
        assertEquals(listOf("findPetsByTags", "placeOrder", "placeOrder"), actions.map { it.operationId })

        execute(actions, ok("""[]"""), ok("""{"id": 1}"""), ok("""{"id": 2}"""))

        val first = body(actions[1])
        assertEquals("111", first.get("petId").asText())
        assertEquals("FIRST", first.get("couponCode").asText())

        val second = body(actions[2])
        assertEquals("222", second.get("petId").asText())
        assertEquals("SECOND", second.get("couponCode").asText())
    }

    private class Reply(val status: Int, val body: String?)

    private fun ok(body: String) = Reply(200, body)

    private fun notFound() = Reply(404, null)

    /**
     * The actions of the individual that [ArazzoWorkflowsService] builds for the workflow.
     */
    private fun actionsOf(workflowId: String): List<RestCallAction> {
        val workflow = arazzoService.arazzoWorkflowsById.getValue(workflowId)
        return arazzoService.buildIndividualFromWorkflow(workflow).seeMainExecutableActions()
    }

    /**
     * What RestFitness does for each action: prepare the request, send it, record the response.
     * Here the response of the i-th action is [replies] i-th.
     */
    private fun execute(actions: List<RestCallAction>, vararg replies: Reply) {
        assertEquals(actions.size, replies.size)

        val runtime = ArazzoRuntime(actions)
        assertNotNull(runtime, "Actions of an Arazzo workflow must get a runtime")

        for ((i, action) in actions.withIndex()) {
            runtime!!.prepareRequest(action)
            runtime.recordResponse(action, result(action, replies[i]))
        }
    }

    private fun result(action: RestCallAction, reply: Reply): RestCallResult {
        val result = RestCallResult(action.getLocalId(), false)
        result.setStatusCode(reply.status)
        if (reply.body != null) {
            result.setBodyType(MediaType.APPLICATION_JSON_TYPE)
            result.setBody(reply.body)
        }
        return result
    }

    private fun pathParam(action: RestCallAction, name: String): String =
        action.parameters.filterIsInstance<PathParam>().single { it.name == name }.primaryGene().getValueAsRawString()

    private fun body(action: RestCallAction): JsonNode =
        ObjectMapper().readTree(action.parameters.filterIsInstance<BodyParam>().single().primaryGene().getValueAsRawString())

    private fun createArazzoService(): ArazzoWorkflowsService {

        val sutInfo = SutInfoDto().apply {
            baseUrlOfSUT = "http://localhost:8080"
            restProblem = RestProblemDto().apply { openApiUrl = File(OPENAPI).absolutePath }
            defaultOutputFormat = SutInfoDto.OutputFormat.JAVA_JUNIT_4
        }

        val args = arrayOf(
            "--blackBox", "false",
            "--seed", "42",
            "--enableArazzoWorkflowSampling", "true",
            "--arazzoLocation", File(ARAZZO).absolutePath,
            "--probOfArazzoSampling", "1.0",
        )

        val controller = object : RemoteController by DummyController() {
            override fun checkConnection() {}
            override fun startSUT() = true
            override fun resetSUT() = true
            override fun startANewSearch() = true
            override fun getSutInfo() = sutInfo
            override fun getControllerInfo() = ControllerInfoDto()
        }

        val fake = object : AbstractModule() {
            override fun configure() {
                bind(RemoteController::class.java).toInstance(controller)
            }
        }

        val injector = LifecycleInjector.builder()
            .withModules(BaseModule(args), RestModule(bindRemote = false), fake)
            .build()
            .createInjector()
        ServiceStarter.startAllServices(injector)

        return injector.getInstance(ArazzoWorkflowsService::class.java)
    }
}
