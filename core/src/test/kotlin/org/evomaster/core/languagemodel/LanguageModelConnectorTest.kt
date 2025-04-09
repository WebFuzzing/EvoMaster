package org.evomaster.core.languagemodel

import com.google.inject.Injector
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.ci.utils.CIUtils
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll

class LanguageModelConnectorTest {

    private lateinit var config: EMConfig
    private lateinit var languageModelConnector: LanguageModelConnector

    companion object {
        @BeforeAll
        @JvmStatic
        fun skipOnGA() {
            CIUtils.skipIfOnGA()
        }
    }

    @Test
    fun testLocalOllamaConnection() {

        val injector: Injector = LifecycleInjector.builder()
            .withModules(BaseModule())
            .build().createInjector()

        config = injector.getInstance(EMConfig::class.java)
        languageModelConnector = injector.getInstance(LanguageModelConnector::class.java)

        // If languageModelName or languageModelURL set to empty, an exception
        // will the thrown.
        config.languageModelName = "llama3.2:latest"
        config.languageModelServerURL = "http://localhost:11434/api/generate"

        languageModelConnector.initialise()

        val answer = languageModelConnector.query("1+1? respond only the answer.")

        Assertions.assertEquals(answer, "2")
    }
}
