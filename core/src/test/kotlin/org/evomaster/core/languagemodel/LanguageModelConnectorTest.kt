package org.evomaster.core.languagemodel

import com.google.inject.Injector
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.ci.utils.CIUtils
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.KGenericContainer
import org.evomaster.core.languagemodel.service.LanguageModelConnector
import org.junit.Test
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll

class LanguageModelConnectorTest {

    private lateinit var config: EMConfig
    private lateinit var languageModelConnector: LanguageModelConnector

    companion object {

        private const val LANGUAGE_MODEL_NAME: String = "llama3.2:latest"

        private val ollama = KGenericContainer("ollama/ollama")
            .withExposedPorts(11434)

        private var ollama_url: String = ""


        @BeforeAll
        @JvmStatic
        fun initClass() {

            CIUtils.skipIfOnGA()

            ollama.start()

            val host = ollama.host
            val port = ollama.getMappedPort(11434)!!

            ollama_url = "http://$host:$port/api/generate"

        }

        @AfterAll
        @JvmStatic
        fun cleanClass() {
            ollama.stop()
        }
    }

    @Test
    fun testLocalOllamaConnection() {

        val injector: Injector = LifecycleInjector.builder()
            .withModules(BaseModule())
            .build().createInjector()

        config = injector.getInstance(EMConfig::class.java)
        config.languageModelConnector = true

        languageModelConnector = injector.getInstance(LanguageModelConnector::class.java)

        // If languageModelName or languageModelURL set to empty, an exception
        // will the thrown.
        config.languageModelName = LANGUAGE_MODEL_NAME
//        config.languageModelServerURL = ollama_url

        languageModelConnector.init()

        val answer = languageModelConnector.query("1+1? respond only the answer.")

        Assertions.assertEquals(answer, "2")
    }
}
