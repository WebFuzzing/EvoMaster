package org.evomaster.core.problem.rest

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Provides
import com.google.inject.Singleton
import com.netflix.governator.guice.LifecycleInjector
import com.webfuzzing.arazzo.models.domain.ArazzoWorkflow
import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.client.java.controller.api.dto.database.operations.*
import org.evomaster.client.java.controller.api.dto.problem.RestProblemDto
import org.evomaster.client.java.controller.api.dto.problem.param.DeriveParamResponseDto
import org.evomaster.client.java.controller.api.dto.problem.param.DerivedParamChangeReqDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationsDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationsResult
import org.evomaster.core.BaseModule
import org.evomaster.core.Main
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.service.ArazzoWorkflowsService
import org.evomaster.core.problem.rest.service.module.RestModule
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.sampler.RestSampler
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class SamplerWithArazzoVerifierTest {

    companion object {
        init {
            Main.applyGlobalJVMSettings()
        }
    }

    private data class TestContext(
        val sampler: RestSampler,
        val arazzoService: ArazzoWorkflowsService,
    )

    @Test
    fun testSamplerWithArazzoProducesValidIndividuals() {
        val context = createTestContext()

        assertTrue(context.arazzoService.arazzoArazzoWorkflows.isNotEmpty(), "Arazzo workflows should be loaded at init")
        assertTrue(context.sampler.numberOfDistinctActions() > 0, "OpenAPI should yield REST actions")

        repeat(10) {
            checkInvariant(context.sampler.sample(forceRandomSample = true))
        }
    }

    /**
     * It is a test that validates the correct formation of expected workflows from a specific Arazzo document.
     * Linear workflows only
     */
    @Test
    fun testSamplerWithArazzoGeneratesValidIndividualWorkflows() {
        val context = createTestContext()

        //apply-coupon
        var workflow = context.arazzoService.arazzoWorkflowsById["apply-coupon"]!!
        var ind = buildIndividualFromWorkflow(context, workflow)
        var actions = ind.seeAllActions().filterIsInstance<RestCallAction>()

        assertEquals(listOf("findPetsByTags", "getPetCoupons", "placeOrder"), actions.map { it.operationId })
        assertEquals(listOf(HttpVerb.GET, HttpVerb.GET, HttpVerb.POST), actions.map { it.verb })
        assertEquals(listOf("/pet/findByTags", "/pet/{petId}/coupons", "/store/order"), actions.map { it.path.toString() })

        //buy-available-pet
        workflow = context.arazzoService.arazzoWorkflowsById["buy-available-pet"]!!
        ind = buildIndividualFromWorkflow(context, workflow)
        actions = ind.seeAllActions().filterIsInstance<RestCallAction>()

        assertEquals(listOf("findPetsByStatus", "placeOrder"), actions.map { it.operationId })
        assertEquals(listOf(HttpVerb.GET, HttpVerb.POST), actions.map { it.verb })
        assertEquals(listOf("/pet/findByStatus", "/store/order"), actions.map { it.path.toString() })

        //place-order
        workflow = context.arazzoService.arazzoWorkflowsById["place-order"]!!
        ind = buildIndividualFromWorkflow(context, workflow)
        actions = ind.seeAllActions().filterIsInstance<RestCallAction>()

        assertEquals(listOf("placeOrder"), actions.map { it.operationId })
        assertEquals(listOf(HttpVerb.POST), actions.map { it.verb })
        assertEquals(listOf("/store/order"), actions.map { it.path.toString() })
    }

    @Test
    fun testSamplerWithArazzoWithOnFailureEndWorkflow() {
        val context = createTestContext()

        //early-exit-tags
        val workflow = context.arazzoService.arazzoWorkflowsById["early-exit-tags"]!!
        val ind = buildIndividualFromWorkflow(context, workflow)
        val actions = ind.seeAllActions().filterIsInstance<RestCallAction>()

        //The workflow has two steps, but since the `onFailure` is of the `end` type, there must be only one action.
        assertEquals(listOf("findPetsByTags"), actions.map { it.operationId })
        assertEquals(listOf(HttpVerb.GET), actions.map { it.verb })
        assertEquals(listOf("/pet/findByTags"), actions.map { it.path.toString() })
    }

    @Test
    fun testSamplerWithArazzoWithOnSuccessGotoStepId() {
        val context = createTestContext()

        //skip-coupons-via-goto
        val workflow = context.arazzoService.arazzoWorkflowsById["skip-coupons-via-goto"]!!
        val ind = buildIndividualFromWorkflow(context, workflow)
        val actions = ind.seeAllActions().filterIsInstance<RestCallAction>()

        //The workflow has only two steps because it skips `getPerCoupons` due to the `goto` in the `onSuccess` handler.
        assertEquals(listOf("findPetsByTags", "placeOrder"), actions.map { it.operationId })
        assertEquals(listOf(HttpVerb.GET, HttpVerb.POST), actions.map { it.verb })
        assertEquals(listOf("/pet/findByTags", "/store/order"), actions.map { it.path.toString() })
    }

    @Test
    fun testSamplerWithArazzoWithOnSuccessGotoOtherWorkkflow() {
        val context = createTestContext()

        //jump-to-other-workflow
        val workflow = context.arazzoService.arazzoWorkflowsById["jump-to-other-workflow"]!!
        val ind = buildIndividualFromWorkflow(context, workflow)
        val actions = ind.seeAllActions().filterIsInstance<RestCallAction>()

        //The workflow jumps to "buy-available-pet," so "getPetCoupons" is not executed.
        assertEquals(listOf("findPetsByTags", "findPetsByStatus", "placeOrder"), actions.map { it.operationId })
        assertEquals(listOf(HttpVerb.GET, HttpVerb.GET, HttpVerb.POST), actions.map { it.verb })
        assertEquals(listOf("/pet/findByTags", "/pet/findByStatus", "/store/order"), actions.map { it.path.toString() })
    }

    @Test
    fun testBranchingWorkflowCoversSuccessAndFailurePaths() {
        val context = createTestContext()
        val workflow = context.arazzoService.arazzoWorkflowsById["branch-success-and-failure"]!!

        val instancesWorkflow = mutableSetOf<List<String>>()
        repeat(200) {
            val ops = context.arazzoService.buildIndividualFromWorkflow(workflow)
                .seeAllActions().filterIsInstance<RestCallAction>().mapNotNull { it.operationId }
            instancesWorkflow.add(ops)
        }

        assertTrue(instancesWorkflow.contains(listOf("findPetsByTags", "placeOrder"))) // onSuccess goto
        assertTrue(instancesWorkflow.contains(listOf("findPetsByTags")))               // onFailure end
        //There are only two possible branches
        assertEquals(2, instancesWorkflow.size)
    }

    @Test
    fun testBranchingWorkflowCoversMultipleOnSuccess() {
        val context = createTestContext()
        val workflow = context.arazzoService.arazzoWorkflowsById["branch-multiple-on-success"]!!

        val instancesWorkflow = mutableSetOf<List<String>>()
        repeat(200) {
            val ops = context.arazzoService.buildIndividualFromWorkflow(workflow)
                .seeAllActions().filterIsInstance<RestCallAction>().mapNotNull { it.operationId }
            instancesWorkflow.add(ops)
        }

        assertTrue(instancesWorkflow.contains(listOf("findPetsByTags", "placeOrder"))) // jump-to-place-order
        assertTrue(instancesWorkflow.contains(listOf("findPetsByTags", "findPetsByStatus", "placeOrder"))) //jump-to-buy-available-pet
        //There are only two possible branches
        assertEquals(2, instancesWorkflow.size)
    }

    @Test
    fun testWorkflowLevelSuccessActionsMergedWithStepOnSuccess() {
        val context = createTestContext()
        val workflow = context.arazzoService.arazzoWorkflowsById["workflow-success-inherit-and-override"]!!

        val instancesWorkflow = mutableSetOf<List<String>>()
        repeat(200) {
            val ops = context.arazzoService.buildIndividualFromWorkflow(workflow)
                .seeAllActions().filterIsInstance<RestCallAction>().mapNotNull { it.operationId }
            instancesWorkflow.add(ops)
        }

        assertTrue(instancesWorkflow.contains(listOf("findPetsByTags", "findPetsByStatus", "placeOrder"))) // overridden alt-checkout
        assertTrue(instancesWorkflow.contains(listOf("findPetsByTags"))) // workflow stop
        //There are only two possible branches
        assertEquals(2, instancesWorkflow.size)    }

    private fun buildIndividualFromWorkflow(context: TestContext, arazzoWorkflow: ArazzoWorkflow): RestIndividual {
        return context.arazzoService.buildIndividualFromWorkflow(arazzoWorkflow)
    }

    private fun createTestContext(): TestContext {
        val openApiPath = testResourcePath("openapi/pet-coupons-openapi.yaml")
        val arazzoPath = testResourcePath("arazzo/pet-coupons-arazzo.yaml")

        val sutInfo = SutInfoDto()
        sutInfo.baseUrlOfSUT = "http://localhost:8080"
        sutInfo.restProblem = RestProblemDto()
        sutInfo.restProblem.openApiUrl = openApiPath
        sutInfo.defaultOutputFormat = SutInfoDto.OutputFormat.JAVA_JUNIT_4

        val controllerInfo = ControllerInfoDto()

        val args = listOf(
            "--blackBox", "false",
            "--seed", "42",
            "--enableArazzoWorkflowSampling", "true",
            "--arazzoLocation", arazzoPath,
            "--probOfArazzoSampling", "1.0",
        )

        val injector = getInjector(sutInfo, controllerInfo, args)
        return TestContext(
            sampler = injector.getInstance(RestSampler::class.java),
            arazzoService = injector.getInstance(ArazzoWorkflowsService::class.java),
        )
    }

    private fun testResourcePath(relativePath: String): String {
        val file = File("../../../core/src/test/resources/$relativePath")
        check(file.exists()) { "Missing test resource: ${file.absolutePath}" }
        return file.absolutePath
    }

    private fun checkInvariant(ind: Individual) {
        assertTrue(ind.isInitialized(), "Sampled individual is not initialized")
        assertTrue(ind.areValidActionLocalIds(), "Sampled individual should have action components which have valid local ids")

        val actions = ind.seeAllActions()

        for(a in actions){

            val topGenes = a.seeTopGenes()
            for(tg in topGenes) {
                assertTrue(tg.isLocallyValid())
                assertTrue(tg.parent !is Gene)
            }
        }
    }

    private fun getInjector(
        sutInfoDto: SutInfoDto,
        controllerInfoDto: ControllerInfoDto,
        args: List<String>,
    ): Injector {
        val base = BaseModule(args.toTypedArray())
        val problemModule = RestModule(bindRemote = false)
        val faker = FakeModule(sutInfoDto, controllerInfoDto)

        return LifecycleInjector.builder()
            .withModules(base, problemModule, faker)
            .build()
            .createInjector()
    }

    private class FakeModule(val sutInfoDto: SutInfoDto?,
                             val controllerInfoDto: ControllerInfoDto?) : AbstractModule() {
        @Provides
        @Singleton
        fun getRemoteController(): RemoteController {
            return FakeRemoteController(sutInfoDto, controllerInfoDto)
        }
    }

    private class FakeRemoteController(
        val sutInfoDto: SutInfoDto?,
        val controllerInfoDto: ControllerInfoDto?) : RemoteController {
        override fun executeDatabaseCommand(dto: DatabaseCommandDto): Boolean {
            return true
        }

        override fun executeDatabaseCommandAndGetQueryResults(dto: DatabaseCommandDto): QueryResultDto? {
            return null
        }

        override fun executeDatabaseInsertionsAndGetIdMapping(dto: DatabaseCommandDto): InsertionResultsDto? {
            return null
        }

        override fun executeMongoDatabaseInsertions(dto: MongoDatabaseCommandDto): MongoInsertionResultsDto? {
            return null
        }

        override fun executeRedisDatabaseInsertions(dto: RedisDatabaseCommandsDto): RedisInsertionResultsDto? {
            return null
        }

        override fun executeDynamoDbInsertions(dto: DynamoDbDatabaseCommandsDto): DynamoDbInsertionResultsDto? {
            return null
        }

        override fun executeCassandraDatabaseInsertions(dto: CassandraDatabaseCommandDto): CassandraInsertionResultsDto? {
            return null
        }

        override fun executeNeo4jInsertions(dto: Neo4jDatabaseCommandsDto): Neo4jInsertionResultsDto? {
            return null
        }

        override fun getSutInfo(): SutInfoDto? {
            return sutInfoDto
        }

        override fun getControllerInfo(): ControllerInfoDto? {
            return controllerInfoDto
        }

        override fun startSUT(): Boolean {
            return true
        }

        override fun stopSUT(): Boolean {
            return true
        }

        override fun resetSUT(): Boolean {
            return true
        }

        override fun checkConnection() {
        }

        override fun startANewSearch(): Boolean {
            return true
        }

        override fun getTestResults(ids: Set<Int>,
                                    ignoreKillSwitch: Boolean,
                                    fullyCovered: Boolean,
                                    descriptiveIds: Boolean): TestResultsDto? {
            return null
        }

        override fun executeNewRPCActionAndGetResponse(actionDto: ActionDto): ActionResponseDto? {
            return null
        }

        override fun postSearchAction(postSearchActionDto: PostSearchActionDto): Boolean {
            return true
        }

        override fun registerNewAction(actionDto: ActionDto): Boolean {
            return true
        }

        override fun address(): String {
            return "localhost:40100"
        }

        override fun close() {
        }

        override fun deriveParams(deriveParams: List<DerivedParamChangeReqDto>): List<DeriveParamResponseDto> {
            return listOf()
        }

        override fun invokeScheduleTasksAndGetResults(dtos: ScheduleTaskInvocationsDto): ScheduleTaskInvocationsResult? {
            return null
        }
    }
}
