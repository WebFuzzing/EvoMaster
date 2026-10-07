package org.evomaster.core.problem.asyncapi.service

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.util.Modules
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.problem.AsyncApiProblemDto
import org.evomaster.core.BaseModule
import org.evomaster.core.remote.service.RemoteController

/**
 * The injector Main would build for an AsyncAPI search, with the driver replaced by a fake.
 */
object AsyncApiTestInjector {

    /**
     * The NCS document, the corpus fixture these suites drive most of their cases from.
     */
    const val NCS = "/asyncapi/sut/ncs-kafka.yaml"

    /**
     * The operations NCS declares, all of them publishable.
     */
    val NCS_OPERATIONS = setOf("checkTriangle", "bessj", "expint", "fisher", "gammq", "remainder")

    /**
     * What a driver declares for a service whose document it hands over as text.
     */
    fun sutInfo(
        schemaText: String,
        declaredFormat: SutInfoDto.OutputFormat = SutInfoDto.OutputFormat.KOTLIN_JUNIT_5
    ): SutInfoDto = SutInfoDto().apply {
        asyncApiProblem = AsyncApiProblemDto().apply { this.schemaText = schemaText }
        defaultOutputFormat = declaredFormat
    }

    /**
     * The options every test over an AsyncAPI service wants, with what one of them asked for
     * instead.
     *
     * An option a caller passes **replaces** the default of the same name rather than joining
     * it: EMConfig refuses an option given twice, so appending both would fail to parse rather
     * than let the later one win. Which is how a test about what gets written turns createTests
     * on where the rest of them want it off.
     *
     * Either spelling counts as the same option, `--name=value` and `--name value`, because
     * both are used in this module and the parser takes either.
     *
     * Here rather than in each test class so that a default added later reaches all of them;
     * the module graph and the fake controller stay each test's own, as they differ on purpose.
     */
    fun argsWith(vararg options: String): Array<String> {

        val named = options.map { it.removePrefix("--").substringBefore('=') }.toSet()

        val defaults = listOf("--seed=42", "--problemType=ASYNCAPI", "--createTests=false")
            .filterNot { it.removePrefix("--").substringBefore('=') in named }

        return (defaults + options).toTypedArray()
    }

    /**
     * An injector for a search driven by [driver], over the real module with the driver
     * swapped, under [argsWith]'s options.
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
}
