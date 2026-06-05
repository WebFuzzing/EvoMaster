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

    private lateinit var executor: ExecutorService

    @PostConstruct
    private fun initService() {

        if(!config.llm){
            //nothing to setup
            return
        }

        try {
            syncModel = LlmSupport.createModel(
                config.llmProvider,
                config.llmApiKey,
                config.llmURL,
                config.llmName,
                config.llmTimeoutSeconds,
                config.llmTemperature
            )

            streamingModel = LlmSupport.createStreamingModel(
                config.llmProvider,
                config.llmApiKey,
                config.llmURL,
                config.llmName,
                config.llmTimeoutSeconds,
                config.llmTemperature
            )
        }catch (e: Exception){
            throw ConfigProblemException("Failed to connect and initialize the chosen LLM: ${e.message}")
        }

        val n = if(config.llmProvider == LlmProvider.OLLAMA) 1 else config.llmThreads
        executor = Executors.newFixedThreadPool(n)
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
        return LlmSupport.chat(syncModel, "", userMessage)
    }

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