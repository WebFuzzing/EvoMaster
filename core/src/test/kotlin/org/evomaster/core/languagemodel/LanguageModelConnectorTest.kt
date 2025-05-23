package org.evomaster.core.languagemodel

import com.google.inject.Injector
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.ci.utils.CIUtils
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.KGenericContainer
import org.evomaster.core.languagemodel.service.LanguageModelConnector
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy

class LanguageModelConnectorTest {

    private lateinit var config: EMConfig

    private lateinit var languageModelConnector: LanguageModelConnector

    companion object {

        /**
         * This chosen based on the two parameters, size and accuracy,
         * after multiple manual trials with other smaller models.
         * The model size is 815MB, so it might take a while to execute the test.
         */
        private const val LANGUAGE_MODEL_NAME: String = "gemma3:1b"

        private val ollama = KGenericContainer("ollama/ollama:latest")
            .withExposedPorts(11434)
            .withEnv("OLLAMA_ORIGINS", "*") // This to allow avoiding CORS filtering.

        private var ollamaURL: String = ""


        @BeforeAll
        @JvmStatic
        fun initClass() {

            // This test takes time to download the LLM model inside
            // docker. So it's wise to avoid running it on CI
            // to reduce execution time.
            CIUtils.skipIfOnGA()

            ollama.start()

            val host = ollama.host
            val port = ollama.getMappedPort(11434)!!

            ollamaURL = "http://$host:$port/"

            ollama.execInContainer("ollama", "pull", LANGUAGE_MODEL_NAME)

            ollama.waitingFor(
                LogMessageWaitStrategy()
                    .withRegEx(".*writing manifest \n success.*")
                    .withTimes(5)
            )
        }

        @AfterAll
        @JvmStatic
        fun cleanClass() {
            ollama.stop()
        }
    }

    @BeforeEach
    fun checkURL() {
        if (!ollama.isRunning) {
            throw IllegalStateException("Ollama container is not running")
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
        config.languageModelServerURL = ollamaURL

        // gemma3:1b returns with a newline character
        val answer = languageModelConnector.queryWithHttpClient("Is A is the first letter in english alphabet? say YES or NO")

        Assertions.assertEquals(answer!!.answer, "YES\n")
    }
}
