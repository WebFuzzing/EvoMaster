package org.evomaster.core.problem.asyncapi.service

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.google.inject.util.Modules
import com.netflix.governator.guice.LifecycleInjector
import com.webfuzzing.asyncapi.access.AsyncApiAccess
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.problem.AsyncApiProblemDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
import org.evomaster.core.BaseModule
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchGlobalState
import org.junit.jupiter.api.Assertions.fail

/**
 * The injector Main would build for an AsyncAPI search, with the driver replaced by a fake.
 */
object AsyncApiTestInjector {

    /**
     * The document most tests here drive a search from.
     */
    const val NCS = "/asyncapi/sut/ncs-kafka.yaml"

    /**
     * Every operation NCS declares.
     */
    val NCS_OPERATIONS = setOf("checkTriangle", "bessj", "expint", "fisher", "gammq", "remainder")

    /**
     * A reply NCS declares for its operations with a floating-point result.
     */
    const val DOUBLE_RESULT = """{"resultAsDouble": 1.5}"""

    /**
     * What a driver says about the service it started. The format it declares matters: for this
     * problem type that is usually where the output format comes from.
     */
    fun sutInfo(
        schemaText: String,
        declaredFormat: SutInfoDto.OutputFormat = SutInfoDto.OutputFormat.KOTLIN_JUNIT_5
    ): SutInfoDto = SutInfoDto().apply {
        asyncApiProblem = AsyncApiProblemDto().apply { this.schemaText = schemaText }
        defaultOutputFormat = declaredFormat
    }

    /**
     * The options these tests run under. One a caller passes replaces the default of that name,
     * in either spelling, since EMConfig refuses an option given twice.
     */
    fun argsWith(vararg options: String): Array<String> {

        fun nameOf(option: String) = option.removePrefix("--").substringBefore('=')

        val named = options.map { nameOf(it) }.toSet()

        val defaults = listOf("--seed=42", "--problemType=ASYNCAPI", "--createTests=false")
            .filterNot { nameOf(it) in named }

        return (defaults + options).toTypedArray()
    }

    /**
     * The real module, with [driver] bound in place of the controller.
     */
    fun create(driver: FakeAsyncApiDriver, vararg options: String): Injector {

        val args = argsWith(*options)

        val fake = object : AbstractModule() {
            override fun configure() {
                bind(RemoteController::class.java).toInstance(driver)
            }
        }

        return LifecycleInjector.builder()
            .withModules(listOf(BaseModule(args), Modules.override(AsyncApiModule()).with(fake)))
            .build().createInjector()
    }

    /**
     * A fake driver for the NCS document, answering every message with [answer].
     */
    fun ncsDriver(answer: (AsyncApiActionDto) -> AsyncApiReplyDto?) =
        FakeAsyncApiDriver(sutInfo(AsyncApiAccess.readFromResource(NCS)), answer)

    /**
     * An individual publishing to [operations] in order, initialised as the search would.
     */
    fun individualOf(injector: Injector, vararg operations: String): AsyncApiIndividual {

        val sampler = injector.getInstance(AsyncApiSampler::class.java)
        val randomness = injector.getInstance(Randomness::class.java)

        val actions = operations.map { name ->
            val template = sampler.seeAvailableActions().first { it.getName() == name }
            (template.copy() as AsyncApiAction).apply { doInitialize(randomness) }
        }

        return AsyncApiIndividual(SampleType.RANDOM, actions.toMutableList()).apply {
            doGlobalInitialize(injector.getInstance(SearchGlobalState::class.java))
        }
    }

    /**
     * [individual] evaluated by the fitness function of [injector].
     */
    fun evaluate(injector: Injector, individual: AsyncApiIndividual): EvaluatedIndividual<AsyncApiIndividual> {

        val fitness = injector.getInstance(Key.get(object : TypeLiteral<FitnessFunction<AsyncApiIndividual>>() {}))

        return fitness.calculateCoverage(individual, modifiedSpec = null)
            ?: fail("the fitness gave up on the individual")
    }

    /**
     * An individual publishing to [operations], evaluated by the fitness function of [injector].
     */
    fun evaluate(injector: Injector, vararg operations: String): EvaluatedIndividual<AsyncApiIndividual> =
        evaluate(injector, individualOf(injector, *operations))
}
