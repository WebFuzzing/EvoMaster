package org.evomaster.core.languagemodel.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.languagemodel.data.AnsweredPrompt
import org.evomaster.core.languagemodel.data.ollama.OllamaModelResponse
import org.evomaster.core.languagemodel.data.ollama.OllamaRequest
import org.evomaster.core.languagemodel.data.ollama.OllamaResponse
import org.evomaster.core.languagemodel.data.Prompt
import org.evomaster.core.languagemodel.data.ollama.OllamaEndpoints
import org.evomaster.core.languagemodel.data.ollama.OllamaRequestVerb
import org.evomaster.core.languagemodel.data.ollama.OllamaStructuredRequest
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.remote.HttpClientFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Objects
import java.util.UUID
import java.util.concurrent.CompletableFuture
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
     * Holds request prompts using [query], [queryAsync], and [addPrompt]
     * as [AnsweredPrompt].
     * Key holds the promptId as type [UUID], and the value is the type of [AnsweredPrompt].
     */
    private var prompts: MutableMap<UUID, AnsweredPrompt> = mutableMapOf()

    private val objectMapper = ObjectMapper()

    private var actualFixedThreadPool = 0

    private lateinit var workerPool: ExecutorService

    private val httpClients: ConcurrentHashMap<Long, Client> = ConcurrentHashMap()

    private var isLanguageModelAvailable: Boolean = false

    companion object {
        private val log: Logger = LoggerFactory.getLogger(LanguageModelConnector::class.java)
    }

    @PostConstruct
    fun init() {
        if (config.languageModelConnector) {
            LoggingUtil.Companion.getInfoLogger().info("Initializing {}", LanguageModelConnector::class.simpleName)

            if (!this.checkModelAvailable()) {
                LoggingUtil.uniqueWarn(
                    log, "${config.languageModelName} is not available in the provided " +
                            "language model server URL: ${config.languageModelServerURL}. " +
                            "Language Model Connector will be disabled."
                )
                return
            } else {
                LoggingUtil.getInfoLogger().info("Language model ${config.languageModelName} is available.")
                isLanguageModelAvailable = true
            }

            actualFixedThreadPool = min(
                config.languageModelConnectorNumberOfThreads,
                Runtime.getRuntime().availableProcessors()
            )
            workerPool = Executors.newFixedThreadPool(
                actualFixedThreadPool
            )
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
     * For testing purposes.
     * @return number of [Client] in [httpClients]
     */
    fun getHttpClientCount() = httpClients.size

    /**
     * To check if the configured language model available.
     * @return Boolean if the configured model available [true], otherwise [false]
     */
    fun isModelAvailable() = isLanguageModelAvailable

    /**
     * Use concurrent programming to make prompt request asynchronously.
     * @return the [CompletableFuture] for the prompt.
     */
    fun queryAsync(prompt: String): CompletableFuture<AnsweredPrompt?> {
        if (!config.languageModelConnector) {
            throw IllegalStateException("Language Model Connector is disabled")
        }

        if (!isLanguageModelAvailable) {
            throw IllegalStateException("Specified Language Model (${config.languageModelName}) is not available in the server.")
        }

        val promptDto = Prompt(getIdForPrompt(), prompt)

        val client = httpClients.getOrPut(Thread.currentThread().id) {
            getHttpClient()
        }

        val future = CompletableFuture.supplyAsync {
            makeQueryWithClient(client, promptDto)
        }

        return future
    }

    /**
     * Added prompt will be queried in a separate thread without
     * blocking the main thread.
     * [getAnswerByPrompt] and [getAnswerById] can be used to retrieve the
     * answers.
     * @return unique prompt identifier as [UUID]
     * @throws [IllegalStateException] if the connector is disabled in [EMConfig]
     */
    fun addPrompt(prompt: String): UUID {
        if (!config.languageModelConnector) {
            throw IllegalStateException("Language Model Connector is disabled")
        }

        if (!isLanguageModelAvailable) {
            throw IllegalStateException("Specified Language Model (${config.languageModelName}) is not available in the server.")
        }

        val promptId = getIdForPrompt()

        val promptDto = Prompt(promptId, prompt)

        val task = Runnable {
            val id = Thread.currentThread().id
            val httpClient = httpClients.getOrPut(id) {
                getHttpClient()
            }
            makeQueryWithClient(httpClient, promptDto)
        }

        workerPool.submit(task)

        return promptId
    }

    /**
     * @return answer for the prompt as [Prompt] if exists
     * @return null if there is no answer for the prompt
     */
    fun getAnswerByPrompt(prompt: String): AnsweredPrompt? {
        return prompts.filter { it.value.prompt.prompt == prompt && !it.value.answer.isNullOrEmpty() }.values.firstOrNull()
    }

    /**
     * @param id unique identifier returned when [addPrompt] invoked.
     * @return answer for the UUID of the prompt
     */
    fun getAnswerById(id: UUID): AnsweredPrompt? {
        return prompts[id]
    }

    /**
     * To query the large language server with a simple prompt.
     * @return answer string from the language model server.
     * @return null if the request failed.
     */
    fun query(prompt: String): AnsweredPrompt? {
        if (!config.languageModelConnector) {
            throw IllegalStateException("Language Model Connector is disabled")
        }

        if (!isLanguageModelAvailable) {
            throw IllegalStateException("Specified Language Model (${config.languageModelName}) is not available in the server.")
        }

        val promptDto = Prompt(getIdForPrompt(), prompt)

        val client = httpClients.getOrPut(Thread.currentThread().id) {
            getHttpClient()
        }

        val response = makeQueryWithClient(client, promptDto)

        return response
    }

    /**
     * @return the given structured request for the prompt.
     */
    fun queryStructured(prompt: String, responseStructure: Any) {
        if (!config.languageModelConnector) {
            throw IllegalStateException("Language Model Connector is disabled")
        }

        if (!isLanguageModelAvailable) {
            throw IllegalStateException("Specified Language Model (${config.languageModelName}) is not available in the server.")
        }

        val promptDto = Prompt(getIdForPrompt(), prompt)

        val client = httpClients.getOrPut(Thread.currentThread().id) {
            getHttpClient()
        }

        val response = makeQueryWithClient(client, promptDto)

        TODO("Requires more time to implement this.")
    }

    private fun checkModelAvailable(): Boolean {
        val url = OllamaEndpoints
            .getTagEndpoint(config.languageModelServerURL)

        val client = httpClients.getOrPut(Thread.currentThread().id) {
            getHttpClient()
        }

        val response = callWithClient(client, url, OllamaRequestVerb.GET)

        if (response != null && response.status == 200 && response.hasEntity()) {
            val body = response.readEntity(String::class.java)

            val bodyObject = objectMapper.readValue(
                body,
                OllamaModelResponse::class.java
            )

            if (bodyObject.models.any { it.name == config.languageModelName }) {
                return true
            }
        }

        return false
    }

    /**
     * @return [AnsweredPrompt] if the request is successfully completed.
     * @return null if the request failed.
     */
    private fun makeQueryWithClient(httpClient: Client, prompt: Prompt, responseStructure: OllamaStructuredRequest? = null): AnsweredPrompt? {
        val languageModelServerURL = OllamaEndpoints
            .getGenerateEndpoint(config.languageModelServerURL)

        val requestBody = if (responseStructure != null) {
            objectMapper.writeValueAsString(
                OllamaStructuredRequest(
                    config.languageModelName,
                    prompt.prompt,
                    false,
                    responseStructure
                )
            )
        } else {
            objectMapper.writeValueAsString(
                OllamaRequest(
                    config.languageModelName,
                    prompt.prompt,
                    false
                )
            )
        }

        val response = callWithClient(httpClient, languageModelServerURL, OllamaRequestVerb.POST, requestBody)

        if (response != null && response.status == 200 && response.hasEntity()) {
            val body = response.readEntity(String::class.java)
            val bodyObject = objectMapper.readValue(
                body,
                OllamaResponse::class.java
            )

            val answer = AnsweredPrompt(
                prompt,
                bodyObject.response
            )

            prompts[prompt.id] = answer

            return answer
        }

        return null
    }

    /**
     * @return [Response] for the request.
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
     * @return unique prompt identifier as [UUID]
     */
    private fun getIdForPrompt(): UUID {
        return UUID.randomUUID()
    }

    /**
     * @return new [Client] from [HttpClientFactory]
     */
    private fun getHttpClient(): Client {
        return HttpClientFactory
            .createTrustingJerseyClient(false, 60_000)
    }

}
