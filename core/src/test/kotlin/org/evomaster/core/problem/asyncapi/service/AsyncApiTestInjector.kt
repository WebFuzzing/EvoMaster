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
     * What a driver declares for a service whose document it hands over as text.
     */
    fun sutInfo(schemaText: String): SutInfoDto = SutInfoDto().apply {
        asyncApiProblem = AsyncApiProblemDto().apply { this.schemaText = schemaText }
        defaultOutputFormat = SutInfoDto.OutputFormat.KOTLIN_JUNIT_5
    }

    fun create(driver: FakeAsyncApiDriver, vararg options: String): Injector {

        val args = arrayOf("--seed=42", "--problemType=ASYNCAPI", "--createTests=false") + options

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
