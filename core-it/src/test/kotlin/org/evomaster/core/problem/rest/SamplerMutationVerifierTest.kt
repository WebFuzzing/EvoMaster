package org.evomaster.core.problem.rest

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Provides
import com.google.inject.Singleton
import com.netflix.governator.guice.LifecycleInjector
import io.swagger.parser.OpenAPIParser
import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionResultsDto
import org.evomaster.client.java.controller.api.dto.database.operations.QueryResultDto
import org.evomaster.client.java.controller.api.dto.problem.RestProblemDto
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.service.ResourceRestModule
import org.evomaster.core.problem.rest.service.ResourceSampler
import org.evomaster.core.remote.service.RemoteController
import org.junit.jupiter.api.Test

class SamplerMutationVerifierTest {


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