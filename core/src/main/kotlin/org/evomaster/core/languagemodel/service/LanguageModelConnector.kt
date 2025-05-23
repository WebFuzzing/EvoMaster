package org.evomaster.core.languagemodel.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.languagemodel.data.AnsweredPrompt
import org.evomaster.core.languagemodel.data.ollama.OllamaModelResponse
import org.evomaster.core.languagemodel.data.ollama.OllamaRequest
import org.evomaster.core.languagemodel.data.ollama.OllamaResponse
import org.evomaster.core.languagemodel.data.Prompt
import org.evomaster.core.languagemodel.data.ollama.OllamaRequestVerb
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.remote.HttpClientFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
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

    private val httpClients: ConcurrentHashMap<Long, Client> = ConcurrentHashMap()

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
            httpClients.values.forEach { it.close() }
            workerPool.shutdown()
            prompts.clear()
        }
    }

    /**
     * To query the large language server asynchronously with a simple prompt.
     */
    fun queryAsync(prompt: String) {
        if (!config.languageModelConnector) {
            return
        }

        validatePrompt(prompt)

        val promptId = getIdForPrompt()

        prompts[promptId] = Prompt(promptId, prompt)

        val task = Runnable {
            val id = Thread.currentThread().id
            val httpClient = httpClients.getOrPut(id) {
                initialiseHttpClient()
            }
            makeQueryWithClient(httpClient, prompt)
        }

        workerPool.submit(task)
    }

    private fun initialiseHttpClient(): Client {
        val client = HttpClientFactory.createTrustingJerseyClient(false, 60_000)

        return client
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
    fun queryWithHttpClient(prompt: String): AnsweredPrompt? {
        if (!config.languageModelConnector) {
            throw IllegalStateException("Language Model Connector is disabled")
        }

        validatePrompt(prompt)

        val client = httpClients.getOrPut(Thread.currentThread().id) {
            initialiseHttpClient()
        }

        val response = makeQueryWithClient(client, prompt)

        return response
    }

    private fun isModelAvailable(): Boolean {
        val url = getLanguageModelServerGenerateURL() + "api/tags"

        val languageModelName = getLanguageModelName()

        val client = httpClients.getOrPut(Thread.currentThread().id) {
            initialiseHttpClient()
        }

        val response = callWithClient(client, url, OllamaRequestVerb.GET)

        if (response != null && response.status == 200 && response.hasEntity()) {
            val body = response.readEntity(String::class.java)

            val bodyObject = objectMapper.readValue(
                body,
                OllamaModelResponse::class.java
            )

            if (bodyObject.models.any { it.name == languageModelName }) {
                return true
            }
        }

        return false
    }

    private fun makeQueryWithClient(httpClient: Client, prompt: String, promptId: UUID? = null): AnsweredPrompt? {
        validatePrompt(prompt)

        val languageModelServerURL = getLanguageModelServerGenerateURL() + "api/generate"
        val languageModelName = getLanguageModelName()

        val requestBody = objectMapper.writeValueAsString(
            OllamaRequest(
                languageModelName.toString(),
                prompt,
                false
            )
        )

        val response = callWithClient(httpClient, languageModelServerURL, OllamaRequestVerb.POST, requestBody)

        if (response != null && response.status == 200 && response.hasEntity()) {
            val body = response.readEntity(String::class.java)
            val bodyObject = objectMapper.readValue(
                body,
                OllamaResponse::class.java
            )

            val answer = AnsweredPrompt(
                prompt,
                bodyObject.response,
                promptId
            )

            return answer
        }

        return null
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
    private fun callWithClient(
        httpClient: Client,
        languageModelServerURL: String,
        requestMethod: OllamaRequestVerb,
        requestBody: String? = "",
    ): Response? {
        val bodyEntity = Entity.entity(requestBody, MediaType.APPLICATION_JSON_TYPE)

        val builder = httpClient.target(languageModelServerURL)
            .request("application/json")

        val invocation = when (requestMethod) {
            OllamaRequestVerb.GET -> builder.buildGet()
            OllamaRequestVerb.POST -> builder.buildPost(bodyEntity)
        }

        val response = try {
            invocation.invoke()
        } catch (e: Exception) {
            LoggingUtil.uniqueWarn(log, "Failed to connect to the language model server. Error: ${e.message}")

            return null
        }

        return response
    }


    /**
     * @return the large language model server URL from EMConfig
     * @return if URL not configured returns the localhost URL for Ollama API.
     */
    private fun getLanguageModelServerGenerateURL(): String {
        if (config.languageModelServerURL.isNullOrEmpty()) {
            throw IllegalArgumentException("Language model URL cannot be empty")
        }

        return config.languageModelServerURL
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

    private fun getIdForPrompt(): UUID {
        return UUID.randomUUID()
    }

    private fun validatePrompt(prompt: String) {
        if (prompt.isBlank()) {
            throw IllegalArgumentException("Prompt cannot be empty")
        }
    }
}
