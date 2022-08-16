package org.evomaster.core.problem.rest

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Provides
import com.google.inject.Singleton
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionResultsDto
import org.evomaster.client.java.controller.api.dto.database.operations.QueryResultDto
import org.evomaster.client.java.controller.api.dto.problem.RestProblemDto
import org.evomaster.core.BaseModule
import org.evomaster.core.problem.rest.service.ResourceRestModule
import org.evomaster.core.problem.rest.service.ResourceSampler
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File


class SamplerVerifierTest {


    @Test
    fun testBase() {

        val resourcePath = "swagger/sut/news.json"

        val sutInfo = SutInfoDto()
        sutInfo.restProblem = RestProblemDto()
        sutInfo.restProblem.openApiSchema = this::class.java.classLoader.getResource(resourcePath).readText()
        sutInfo.defaultOutputFormat = SutInfoDto.OutputFormat.JAVA_JUNIT_4

        val controllerInfo = ControllerInfoDto()

        val injector = getInjector(sutInfo, controllerInfo)

        val sampler = injector.getInstance(ResourceSampler::class.java)

        sampler.sample() //should not crash
    }

    @TestFactory
    fun testSamplingFromAllSchemasUnderResources(): Collection<DynamicTest> {

        return scanForSchemas().map {
            DynamicTest.dynamicTest(it) { runInvariantCheck(it, 1000)}
        }.toList()
    }

    private fun scanForSchemas() : List<String>{
        val relativePath = "../core/src/test/resources/swagger"
        val target = File(relativePath)
        if (!target.exists()) {
            throw IllegalStateException("Swagger resource folder does not exist: ${target.absolutePath}")
        }

        return target.walk()
                .filter { it.isFile }
                .filter { !it.name.endsWith("features_service_null.json") } //issue with parser
                .filter { !it.name.endsWith("trace_v2.json") } // no actions are parsed
                .map {
                    val s = it.path.replace("\\", "/")
                            .replace(relativePath, "swagger")
                    s
                }.toList()
    }

    private fun runInvariantCheck(resourcePath: String, iterations: Int){

        val sutInfo = SutInfoDto()
        sutInfo.restProblem = RestProblemDto()
        sutInfo.restProblem.openApiSchema = this::class.java.classLoader.getResource(resourcePath).readText()
        sutInfo.defaultOutputFormat = SutInfoDto.OutputFormat.JAVA_JUNIT_4

        val controllerInfo = ControllerInfoDto()

        val injector = getInjector(sutInfo, controllerInfo, listOf("--seed","42"))

        val sampler = injector.getInstance(ResourceSampler::class.java)


        for(i in 0..iterations) {
            val ind = sampler.sample()
            checkInvariant(ind)
        }
    }

    private fun checkInvariant(ind: Individual){

        assertTrue(ind.isInitialized(), "Sampled individual is not initialized")
        assertTrue(ind.areValidLocalIds(), "Sampled individual should have action components which have valid local ids")

        val actions = ind.seeAllActions()

        for(a in actions){

            val topGenes = a.seeTopGenes()
            for(tg in topGenes) {
                assertTrue(tg.isLocallyValid())
                assertTrue(tg.parent !is Gene)
            }
        }

        //TODO check global validity

        //TODO more checks, eg validity
    }

    private fun getInjector(
            sutInfoDto: SutInfoDto?,
            controllerInfoDto: ControllerInfoDto?,
            args: List<String> = listOf()): Injector {

        val base = BaseModule(args.toTypedArray())
        val problemModule = ResourceRestModule(false)
        val faker = FakeModule(sutInfoDto, controllerInfoDto)

        return LifecycleInjector.builder()
                .withModules(base, problemModule, faker)
                .build()
                .createInjector()
    }

    private class FakeModule(val sutInfoDto: SutInfoDto?,
                             val controllerInfoDto: ControllerInfoDto?) : AbstractModule() {
        //        override fun configure() {
//            bind(RemoteController::class.java)
//                    .to(FakeRemoteController::class.java)
//                    .asEagerSingleton()
//        }
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

        override fun getTestResults(ids: Set<Int>, ignoreKillSwitch: Boolean): TestResultsDto? {
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
    }
}