package org.evomaster.core.search.algorithms

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.algorithms.onemax.OneMaxIndividual
import org.evomaster.core.search.algorithms.onemax.OneMaxModule
import org.evomaster.core.search.algorithms.onemax.OneMaxSampler
import org.evomaster.core.search.service.ExecutionPhaseController
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.client.java.controller.api.dto.problem.param.DeriveParamResponseDto
import org.evomaster.client.java.controller.api.dto.problem.param.DerivedParamChangeReqDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationsDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationsResult
import org.evomaster.client.java.controller.api.dto.database.operations.*
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.client.java.controller.api.dto.ActionResponseDto
import org.evomaster.client.java.controller.api.dto.ControlDependenceGraphDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DynaMosaAlgorithmTest {

    private lateinit var injector: Injector

    @BeforeEach
    fun setUp() {
        injector = LifecycleInjector.builder()
            .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule(), DynaMosaTestModule()))
            .build().createInjector()
    }

    @Test
    fun testDynamosaFindsOptimumOnOneMax() {
        val dynamosa = injector.getInstance(
            Key.get(object : TypeLiteral<DynaMosaAlgorithm<OneMaxIndividual>>() {})
        )

        val config = injector.getInstance(EMConfig::class.java)
        config.maxEvaluations = 10_000
        config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

        val epc = injector.getInstance(ExecutionPhaseController::class.java)
        epc.startSearch()
        val solution = dynamosa.search()
        epc.finishSearch()

        assertEquals(1, solution.individuals.size)
        assertEquals(
            OneMaxSampler.DEFAULT_N.toDouble(),
            solution.overall.computeFitnessScore(),
            0.001
        )
    }
}

private class DynaMosaTestModule : AbstractModule() {
    override fun configure() {
        bind(RemoteController::class.java).toInstance(DummyRemoteController())
    }
}

private class DummyRemoteController : RemoteController {
    override fun getSutInfo(): SutInfoDto? = null
    override fun getControllerInfo(): ControllerInfoDto? = null
    override fun startSUT(): Boolean = true
    override fun stopSUT(): Boolean = true
    override fun resetSUT(): Boolean = true
    override fun checkConnection() {}
    override fun startANewSearch(): Boolean = true
    override fun getTestResults(
        ids: Set<Int>,
        ignoreKillSwitch: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean
    ): TestResultsDto? = null

    override fun executeNewRPCActionAndGetResponse(actionDto: ActionDto): ActionResponseDto? = null
    override fun postSearchAction(postSearchActionDto: PostSearchActionDto): Boolean = true
    override fun registerNewAction(actionDto: ActionDto): Boolean = true
    override fun address(): String = "dummy"
    override fun close() {}
    override fun deriveParams(deriveParams: List<DerivedParamChangeReqDto>): List<DeriveParamResponseDto> = emptyList()
    override fun getControlDependenceGraphs(): List<ControlDependenceGraphDto> = emptyList()

    override fun executeDatabaseCommand(dto: DatabaseCommandDto): Boolean = true
    override fun executeDatabaseCommandAndGetQueryResults(dto: DatabaseCommandDto): QueryResultDto? = null
    override fun executeDatabaseInsertionsAndGetIdMapping(dto: DatabaseCommandDto): InsertionResultsDto? = null
    override fun executeMongoDatabaseInsertions(dto: MongoDatabaseCommandDto): MongoInsertionResultsDto? = null
    override fun invokeScheduleTasksAndGetResults(dtos: ScheduleTaskInvocationsDto): ScheduleTaskInvocationsResult? = null
}

