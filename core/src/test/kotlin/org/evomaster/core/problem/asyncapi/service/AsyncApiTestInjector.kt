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
    fun sutInfo(schemaText: String): SutInfoDto = SutInfoDto().apply {
        asyncApiProblem = AsyncApiProblemDto().apply { this.schemaText = schemaText }
        defaultOutputFormat = SutInfoDto.OutputFormat.KOTLIN_JUNIT_5
    }

    fun create(driver: FakeAsyncApiDriver, vararg options: String): Injector {

        /*
            Most suites here search without writing tests, but one is about what gets written,
            and the option cannot be given twice.
         */
        val defaults = listOf("--seed=42", "--problemType=ASYNCAPI", "--createTests=false")
            .filterNot { d -> options.any { it.substringBefore('=') == d.substringBefore('=') } }

        val args = (defaults + options).toTypedArray()

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
