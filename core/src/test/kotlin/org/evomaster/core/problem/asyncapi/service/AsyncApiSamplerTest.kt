package org.evomaster.core.problem.asyncapi.service

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.netflix.governator.guice.LifecycleInjector
import com.webfuzzing.asyncapi.access.AsyncApiAccess
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.problem.AsyncApiProblemDto
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.external.service.DummyController
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class AsyncApiSamplerTest {

    companion object {
        private const val NCS = "/asyncapi/sut/ncs-kafka.yaml"

        private val NCS_OPERATIONS = setOf("checkTriangle", "bessj", "expint", "fisher", "gammq", "remainder")
    }

    /**
     * A driver that only answers what the sampler asks at start-up: that the service is running,
     * and where its AsyncAPI document is.
     */
    private class FakeController(private val info: SutInfoDto) : RemoteController by DummyController() {
        override fun checkConnection() {}
        override fun startSUT() = true
        override fun getSutInfo() = info
    }

    private fun sutInfo(configure: AsyncApiProblemDto.() -> Unit) = SutInfoDto().apply {
        asyncApiProblem = AsyncApiProblemDto().apply(configure)
        defaultOutputFormat = SutInfoDto.OutputFormat.KOTLIN_JUNIT_5
    }

    private fun sampler(info: SutInfoDto, vararg options: String): AsyncApiSampler {

        val args = arrayOf("--seed=42", "--problemType=ASYNCAPI") + options

        val modules = listOf(BaseModule(args), object : AbstractModule() {
            override fun configure() {
                bind(RemoteController::class.java).toInstance(FakeController(info))
                bind(AsyncApiSampler::class.java).asEagerSingleton()
            }
        })

        val injector: Injector = LifecycleInjector.builder().withModules(modules).build().createInjector()

        return injector.getInstance(AsyncApiSampler::class.java)
    }

    private fun ncsSampler(vararg options: String) =
        sampler(sutInfo { schemaText = AsyncApiAccess.readFromResource(NCS) }, *options)

    @Test
    fun testOneActionPerOperationTheDriverDescribes() {

        val sampler = ncsSampler("--blackBox=false")

        assertEquals(NCS_OPERATIONS, sampler.seeAvailableActions().map { it.getName() }.toSet())
    }

    @Test
    fun testTheSameDocumentServesBlackBoxMode() {

        /*
            No black-box branch in the sampler: the document arrives through the driver either
            way, since the driver is what holds the connection to the broker.
         */
        val sampler = ncsSampler("--blackBox=true")

        assertEquals(NCS_OPERATIONS.size, sampler.numberOfDistinctActions())
    }

    @Test
    fun testTheDocumentCanBeFetchedFromWhereTheDriverSaysItIs(@TempDir dir: Path) {

        val file = dir.resolve("asyncapi.yaml")
        Files.write(file, AsyncApiAccess.readFromResource(NCS).toByteArray(StandardCharsets.UTF_8))

        val sampler = sampler(sutInfo { schemaLocation = file.toString() }, "--blackBox=false")

        assertEquals(NCS_OPERATIONS.size, sampler.numberOfDistinctActions())
    }

    @Test
    fun testARandomIndividualPublishesBetweenOneAndMaxTestSizeMessages() {

        val sampler = ncsSampler("--blackBox=false", "--maxTestSize=4")

        repeat(50) {
            val individual = sampler.sample(forceRandomSample = true)
            val actions = individual.seeMainExecutableActions()

            assertTrue(actions.size in 1..4, "got ${actions.size} messages")
            assertTrue(actions.all { it is AsyncApiAction })
            assertTrue(actions.all { it.isInitialized() }, "an action was sampled with uninitialized genes")
            assertTrue(individual.seeInitializingActions().isEmpty())
        }
    }

    @Test
    fun testEveryOperationIsTriedOnceBeforeMessagesAreCombined() {

        val sampler = ncsSampler("--blackBox=false", "--probOfSmartSampling=1.0")

        val first = (1..NCS_OPERATIONS.size).map {
            assertTrue(sampler.hasSpecialInit(), "ran out of single-message individuals early")
            sampler.sample()
        }

        //six individuals of one message each, covering the six operations
        assertTrue(first.all { it.seeMainExecutableActions().size == 1 })
        assertEquals(NCS_OPERATIONS, first.map { it.seeMainExecutableActions().single().getName() }.toSet())

        assertFalse(sampler.hasSpecialInit())
    }

    @Test
    fun testADocumentThatIsNotThereIsAProblemWithTheSut(@TempDir dir: Path) {

        //the likeliest mistake in a driver: a path that is right on the author's machine only
        val e = assertThrows(Throwable::class.java) {
            sampler(sutInfo { schemaLocation = dir.resolve("absent.yaml").toString() }, "--blackBox=false")
        }

        //Guice and Governator wrap whatever the sampler throws while it is being created
        val causes = generateSequence(e) { it.cause }.toList()
        assertTrue(causes.any { it is SutProblemException }, causes.joinToString { it.toString() })
    }
}
