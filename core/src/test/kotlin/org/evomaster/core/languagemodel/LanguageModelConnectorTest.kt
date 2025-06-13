package org.evomaster.core.languagemodel

import com.fasterxml.jackson.databind.ObjectMapper
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

    val injector: Injector = LifecycleInjector.builder()
        .withModules(BaseModule())
        .build().createInjector()

    private lateinit var languageModelConnector: LanguageModelConnector

    companion object {

        /**
         * This chosen based on the two parameters, size and accuracy,
         * after multiple manual trials with other smaller models.
         * The model size is 815MB, so it might take a while to execute the test.
         */
        private const val LANGUAGE_MODEL_NAME: String = "gemma3:1b"

        private const val PROMPT = "Is A is the first letter in english alphabet? say YES or NO"

        private const val PROMPT_STRUCTURED =
            "What is the capital city of Norway and what are the languages. Also, is Norway close to Antarctic continent?"

        private const val EXPECTED_ANSWER = "YES\n"

        private const val EXPECTED_STRUCTURED_ANSWER = "Oslo"

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
    fun prepareForTest() {
        if (!ollama.isRunning) {
            throw IllegalStateException("Ollama container is not running")
        }

        config = injector.getInstance(EMConfig::class.java)
        config.languageModelConnector = true
        // If languageModelName or languageModelURL set to empty, an exception
        // will the thrown.
        config.languageModelName = LANGUAGE_MODEL_NAME
        config.languageModelServerURL = ollamaURL

        languageModelConnector = injector.getInstance(LanguageModelConnector::class.java)
    }

    @Test
    fun testLocalOllamaConnection() {
        // gemma3:1b returns with a newline character
        val answer = languageModelConnector.query(PROMPT)

        Assertions.assertEquals(EXPECTED_ANSWER, answer!!.answer)
        // We use HttpClient for two purposes by default when make a query.
        // First time connector checks for the model availability,
        // second to make the prompt query.
        // This check validates if there is a client it is repurposed for the second query.
        Assertions.assertEquals(1, languageModelConnector.getHttpClientCount())
    }

    @Test
    fun testConcurrentRequests() {
        // gemma3:1b returns with a newline character
        val future = languageModelConnector.queryAsync(PROMPT)

        future.thenAccept { result ->
            Assertions.assertEquals(EXPECTED_ANSWER, result!!.answer)
            Assertions.assertEquals(1, languageModelConnector.getHttpClientCount())
        }
    }

    @Test
    fun testQueriedPrompts() {
        val promptId = languageModelConnector.addPrompt(PROMPT)

        Thread.sleep(3000)

        val result = languageModelConnector.getAnswerById(promptId)

        Assertions.assertEquals(result!!.answer, EXPECTED_ANSWER)
        Assertions.assertEquals(2, languageModelConnector.getHttpClientCount())
    }

    @Test
    fun testStructuredRequest() {
        val objectMapper = ObjectMapper()

        val responseFormat = languageModelConnector.parseObjectToResponseFormat(
            ResponseDto::class,
            listOf("city", "languages", "antarctic")
        )

        val answer = languageModelConnector.query(PROMPT_STRUCTURED, responseFormat)

        val dto: ResponseDto = objectMapper.readValue(
            answer!!.answer,
            ResponseDto::class.java
        )

        Assertions.assertEquals(EXPECTED_STRUCTURED_ANSWER, dto.city)
        Assertions.assertTrue(dto.languages.contains("Norwegian"))
        Assertions.assertEquals(false, dto.antarctic)
    }
}
