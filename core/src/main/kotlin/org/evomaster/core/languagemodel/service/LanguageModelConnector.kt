package org.evomaster.core.languagemodel.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.languagemodel.data.ollama.OllamaModelResponse
import org.evomaster.core.languagemodel.data.ollama.OllamaRequest
import org.evomaster.core.languagemodel.data.ollama.OllamaResponse
import org.evomaster.core.languagemodel.data.Prompt
import org.evomaster.core.logging.LoggingUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.math.min

/**
 * A utility service designed to handle large language model server
 * related functions.
 *
 * Designed to work with Ollama (version 0.7.0).
 */
class LanguageModelConnector {


    @Inject
    private lateinit var config: EMConfig

    /**
     * Key: UUID assigned when queryAsync invoked.
     * Value: [Prompt]
     */
    private var prompts: MutableMap<UUID, Prompt> = mutableMapOf()

    private val objectMapper = ObjectMapper()

    private var actualFixedThreadPool = 0

    private lateinit var workerPool: ExecutorService

    companion object {
        private val log: Logger = LoggerFactory.getLogger(LanguageModelConnector::class.java)
    }

    @PostConstruct
    fun init() {
        LoggingUtil.Companion.getInfoLogger().info("Initializing {}", LanguageModelConnector::class.simpleName)

        if (config.languageModelConnector) {
            actualFixedThreadPool = min(
                config.languageModelConnectorNumberOfThreads,
                Runtime.getRuntime().availableProcessors()
            )
            workerPool = Executors.newFixedThreadPool(
                actualFixedThreadPool
            )

            if (!this.isModelAvailable()) {
                throw IllegalStateException("${config.languageModelName} is not available in the provided URL.")
            } else {
                LoggingUtil.getInfoLogger().info("Language model ${config.languageModelName} is available.")
            }
        }
    }

    @PreDestroy
    private fun preDestroy() {
        if (config.languageModelConnector) {
            shutdown()
        }
    }

    private fun shutdown() {
        Lazy.assert { config.languageModelConnector }
        workerPool.shutdown()
        prompts.clear()
    }

    /**
     * To query the large language server asynchronously with a simple prompt.
     */
    fun queryAsync(prompt: String) {
        if (!config.languageModelConnector) {
            return
        }

        validatePrompt(prompt)

        val id = getId()

        prompts[id] = Prompt(id, prompt)

        val task = Runnable {
            makeQuery(prompt)
        }

        workerPool.submit(task)
    }

    /**
     * @return answer for the prompt as [Prompt] if exists
     * @return null if there is no answer for the prompt
     */
    fun getAnswerByPrompt(prompt: String): Prompt? {
        return prompts.filter { it.value.prompt == prompt && it.value.hasAnswer() }.values.firstOrNull()
    }

    /**
     * @return answer for the UUID of the prompt
     */
    fun getAnswerById(id: UUID): Prompt? {
        return prompts[id]
    }

    /**
     * To query the large language server with a simple prompt.
     * @return answer string from the language model server
     */
    fun query(prompt: String): String? {
        if (!config.languageModelConnector) {
            throw IllegalStateException("Language Model Connector is disabled")
        }

        validatePrompt(prompt)

        return makeQuery(prompt)
    }

    private fun makeQuery(prompt: String, id: UUID? = null): String? {
        validatePrompt(prompt)

        val languageModelServerURL = getLanguageModelServerGenerateURL()

        val languageModelName = getLanguageModelName()

        val requestBody = objectMapper.writeValueAsString(
            OllamaRequest(
                prompt = prompt,
                stream = false,
                model = languageModelName.toString()
            )
        )

        val response = call(languageModelServerURL, requestBody)

        if (id != null) {
            if (prompts.contains(id)) {
                val existingPrompt = prompts[id]

                if (existingPrompt != null) {
                    existingPrompt?.answer = response
                }
            }
        }

        return response
    }

    private fun isModelAvailable(): Boolean {
        val url = config.languageModelServerURL + "/api/tags"

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 1000
            connection.doInput = true
            connection.useCaches = false

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                LoggingUtil.uniqueWarn(
                    log,
                    "Failed to connect to language model server with status code ${connection.responseCode}"
                )
                return false
            }

            val response = objectMapper.readValue(
                connection.inputStream,
                OllamaModelResponse::class.java
            )

            if (response.models.any { it.name == config.languageModelName }) {
                return true
            }

        } catch (e: Exception) {
            return false
        }

        return false
    }

    /**
     * Private method to make the call to the large language model server.
     *
     * Note: If you are using Ollama as a server, please make sure to set the
     * CORS origin for Ollama on the host operating system.
     *
     * Reference:
     * https://medium.com/dcoderai/how-to-handle-cors-settings-in-ollama-a-comprehensive-guide-ee2a5a1beef0
     */
    private fun call(languageModelServerURL: String, requestBody: String): String? {
        try {
            val connection = URL(languageModelServerURL).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            // TODO: set to avoid long running calls
            connection.connectTimeout = 4000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.useCaches = false
            connection.doInput = true
            connection.doOutput = true

            val writer = DataOutputStream(connection.outputStream)
            writer.writeBytes(requestBody)
            writer.flush()
            writer.close()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                LoggingUtil.uniqueWarn(
                    log,
                    "Failed to connect to language model server with status code ${connection.responseCode}"
                )
                return null
            }

            // This is designed to use the non-stream outputs.
            // If stream is needed, consider implementing a
            // different method to handle stream outputs.
            val response = objectMapper.readValue(
                connection.inputStream,
                OllamaResponse::class.java
            )

            return response.response
        } catch (e: Exception) {
            LoggingUtil.uniqueWarn(log, "Failed to connect to language model server: ${e.message}.")

            return null
        }
    }

    /**
     * @return the large language model server URL from EMConfig
     * @return if URL not configured returns the localhost URL for Ollama API.
     */
    private fun getLanguageModelServerGenerateURL(): String {
        if (config.languageModelServerURL.isNullOrEmpty()) {
            throw IllegalArgumentException("Language model URL cannot be empty")
        }

        return config.languageModelServerURL + "api/generate"
    }

    /**
     * Private method, returns the large language model server name from
     * EMConfig, otherwise return llama3.2:latest.
     *
     * TODO: Failsafe model name yet to be decided
     */
    private fun getLanguageModelName(): String {
        if (config.languageModelName.isNullOrEmpty()) {
            throw IllegalArgumentException("Language model name cannot be empty")
        }

        return config.languageModelName
    }

    private fun getId(): UUID {
        return UUID.randomUUID()
    }

    private fun validatePrompt(prompt: String) {
        if (prompt.isBlank()) {
            throw IllegalArgumentException("Prompt cannot be empty")
        }
    }
}
