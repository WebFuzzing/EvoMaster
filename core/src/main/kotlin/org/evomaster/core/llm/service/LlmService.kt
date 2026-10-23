package org.evomaster.core.llm.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import org.evomaster.core.EMConfig
import org.evomaster.core.config.ConfigProblemException
import org.evomaster.core.llm.FieldInfo
import org.evomaster.core.llm.LlmProvider
import org.evomaster.core.llm.LlmSupport
import org.evomaster.core.llm.Prompts
import org.evomaster.core.llm.orcarouter.OrcaRouterCatalogClient
import org.evomaster.core.llm.orcarouter.OrcaRouterCatalogSource
import org.evomaster.core.llm.orcarouter.OrcaRouterCredentials
import org.evomaster.core.llm.orcarouter.OrcaRouterDefaults
import org.evomaster.core.llm.orcarouter.OrcaRouterModel
import org.evomaster.core.llm.orcarouter.OrcaRouterModelSelector
import org.evomaster.core.llm.orcarouter.OrcaRouterOrigins
import org.evomaster.core.logging.LoggingUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.annotation.PostConstruct
import javax.inject.Inject

class LlmService {

    @Inject
    private lateinit var config: EMConfig

    private lateinit var streamingModel: StreamingChatModel

    private lateinit var syncModel: ChatModel

    /**
     * We can make several requests in parallel toward an LLM.
     * In the end, these are I/O bound, and could take fews seconds each.
     * So make sense to do in parallel and asynchronously, especially when we do not need to wait
     * for the responses.
     */
    private lateinit var executor: ExecutorService

    @PostConstruct
    private fun initService() {

        if(!config.llm){
            //nothing to setup
            return
        }

        val apiKey = if (config.llmProvider == LlmProvider.ORCAROUTER) {
            OrcaRouterCredentials.resolve(config.llmApiKey)
                ?: throw ConfigProblemException(
                    "OrcaRouter requires an API key: pass --llmApiKey <sk-orca-…>, " +
                            "set ORCA_API_KEY, or create one at ${OrcaRouterOrigins.PUBLIC_AUTH_ORIGIN}/console"
                )
        } else {
            config.llmApiKey
        }

        val modelName = resolveModelName(apiKey)

        try {
            syncModel = LlmSupport.createModel(
                config.llmProvider,
                apiKey,
                config.llmURL,
                modelName,
                config.llmTimeoutSeconds,
                config.llmTemperature
            )

            streamingModel = LlmSupport.createStreamingModel(
                config.llmProvider,
                apiKey,
                config.llmURL,
                modelName,
                config.llmTimeoutSeconds,
                config.llmTemperature
            )
        }catch (e: Exception){
            throw ConfigProblemException(
                "Failed to connect and initialize the chosen LLM: ${redactSecret(e.message, apiKey)}"
            )
        }

        /*
            If user asks to use LLM, then verify immediately if it works, by making a call
         */
        val response = try{
            chat(Prompts.GREETINGS)
        }catch (e: Exception){
            if (config.llmProvider == LlmProvider.ORCAROUTER &&
                OrcaRouterCredentials.isModelAccessDenied(e)) {
                throw ConfigProblemException(
                    "The OrcaRouter key cannot use model '$modelName'. " +
                            "Choose an accessible model with --llmName (see --llmListModels true), " +
                            "or manage this key's model access at ${OrcaRouterOrigins.PUBLIC_AUTH_ORIGIN}/console. " +
                            "Provider said: ${redactSecret(e.message, apiKey)}"
                )
            }
            if (config.llmProvider == LlmProvider.ORCAROUTER &&
                OrcaRouterCredentials.isAuthenticationFailure(e)) {
                throw ConfigProblemException(
                    OrcaRouterCredentials.authenticationFailureHint(redactSecret(e.message, apiKey))
                )
            }
            throw ConfigProblemException(
                "Failed to connect to LLM: ${redactSecret(e.message, apiKey)}"
            )
        }
        LoggingUtil.getInfoLogger().info("Connected LLM replies with: $response")

        val n = if(config.llmProvider == LlmProvider.OLLAMA) 1 else config.llmThreads
        executor = Executors.newFixedThreadPool(n)
    }

    /**
     * For OrcaRouter, the model must come from the compatible chat catalog (live, or the
     * verified fallback when discovery is unavailable). A free-text name that is not in that
     * list is rejected here rather than silently failing on the first request.
     */
    private fun resolveModelName(apiKey: String?): String? {

        if (config.llmProvider != LlmProvider.ORCAROUTER) {
            return config.llmName
        }

        val baseUrl = try {
            OrcaRouterOrigins.resolveApiBase(config.llmURL)
        } catch (e: IllegalArgumentException) {
            throw ConfigProblemException("Invalid OrcaRouter URL: ${e.message}")
        }

        val catalog = OrcaRouterCatalogClient(
            baseUrl = baseUrl,
            timeoutSeconds = config.llmTimeoutSeconds.coerceIn(1, 30)
        ).fetchChat(apiKey)

        if (catalog.degraded) {
            LoggingUtil.getInfoLogger().warn(
                "OrcaRouter model discovery is degraded (${catalog.note}); " +
                        "using the verified fallback catalog"
            )
        }

        val selected: OrcaRouterModel = try {
            OrcaRouterModelSelector.selectChatModel(catalog, config.llmName)
        } catch (e: IllegalArgumentException) {
            throw ConfigProblemException(e.message ?: "Invalid OrcaRouter model")
        }

        LoggingUtil.getInfoLogger().info(
            "OrcaRouter model '${selected.id}' selected from " +
                    if (catalog.source == OrcaRouterCatalogSource.LIVE) "the live catalog" else "the verified fallback catalog"
        )
        if (selected.id != OrcaRouterDefaults.DEFAULT_MODEL && config.llmName == null) {
            LoggingUtil.getInfoLogger().info(
                "The OrcaRouter cyber-tuned model '${OrcaRouterDefaults.DEFAULT_MODEL}' is not available " +
                        "for this account; using '${selected.id}'"
            )
        }
        return selected.id
    }

    /**
     * Removes the credential from any provider message before it reaches logs or exceptions.
     */
    private fun redactSecret(message: String?, secret: String?): String? {
        if (message == null) {
            return null
        }
        if (secret.isNullOrBlank()) {
            return message
        }
        return message.replace(secret, OrcaRouterCredentials.redact(secret))
    }

    fun shutdown() {
        executor.shutdown()
    }

    private fun checkUsingLLM(){
        if(!config.llm){
            throw IllegalStateException("LLM is not in use")
        }
    }

    fun chatAsync(userMessage: String): Future<String> {
        checkUsingLLM()

        return LlmSupport.chatAsync(streamingModel, userMessage)
    }

    fun chat(userMessage: String) : String{
        return LlmSupport.chat(syncModel, userMessage)
    }

    fun chat(systemMessage: String, userMessage: String) : String{
        return LlmSupport.chat(syncModel, systemMessage, userMessage)
    }

    /**
     * Query the LLM for input data examples, based on parameter name and optional description.
     * These calls will be made in parallel, for each [fields] entry.
     * When a task is completed, the [callback] is executed with the collected examples.
     */
    fun askForNewExamples(fields: Collection<FieldInfo>, callback: (name: String, examples: Collection<String>) -> Unit){

        fields.toList()
            .sortedBy { it.name }
            .forEach { entry ->
                executor.submit { askForExamplesTask(entry.name, entry.description, callback) }
            }
        //here we do not wait... updates are done asynchronously
    }

    private fun askForExamplesTask(
        name: String,
        description: String?,
        callback: (name: String, examples: Collection<String>) -> Unit
    ){
        /*
            Note: this is the same approach used to build the dictionary
         */
        val mapper = ObjectMapper()
        val prompt = Prompts.getPromptForNameDescription(name, description)
        var result = LlmSupport.chat(syncModel, prompt.first, prompt.second)
        val list = try {
            mapper.readValue(result, object : TypeReference<List<String>>() {})
        } catch (e: Exception) {
            try {
                val failed = Prompts.getPromptForFailedName(e.toString())
                result = LlmSupport.chat(syncModel, failed.first, failed.second)
                mapper.readValue(result, object : TypeReference<List<String>>() {})
            } catch (e: Exception) {
                throw e
            }
        }
        callback(name, list)
    }
}
