package org.evomaster.core.llm

import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.azure.AzureOpenAiChatModel
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.request.ResponseFormat
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import java.util.concurrent.CompletableFuture


object LlmSupport {

    fun chatAsync(model: StreamingChatModel, messages: List<ChatMessage> ) : CompletableFuture<String> {
        val future = CompletableFuture<String>()

        model.chat(messages, object : StreamingChatResponseHandler {
            override fun onCompleteResponse(response: ChatResponse) {
                future.complete(response.aiMessage().text())
            }
            override fun onError(error: Throwable?) {
                future.completeExceptionally(error)
            }
            // onPartialResponse can be ignored
        })
        return future

    }

    fun chat(model: ChatModel, systemMessage: String, userMessage: String): String {

        val s = SystemMessage.systemMessage(systemMessage)
        val u = UserMessage.userMessage(userMessage)

        return model.chat(listOf(s,u)).aiMessage().text()
    }


    fun chatAsync(model: StreamingChatModel, userMessage: String): CompletableFuture<String> {
        val u = UserMessage.userMessage(userMessage)
        return chatAsync(model, listOf(u))
    }

    /*
        WARNING LangChan4J really suck at OO design.
        Forced here to copy&paste lot of code, as there is no common shared class
     */

    fun createModel(
        provider: LlmProvider,
        apiKey: String? = null,
        url: String? = null,
        modelName: String? = null,
        timeoutSeconds: Long = 60,
        temperature: Double = 0.3,
    ): ChatModel {

        if(timeoutSeconds < 0){
            throw IllegalArgumentException("Negative timeout: $timeoutSeconds")
        }
        if(temperature !in 0.0..2.0){
            throw IllegalArgumentException("Invalid temperature: $temperature")
        }

        /*
            unfortunately it does not seem these builders share any common superclass :(
            so most of the configurations need to be copy&paste
         */

        return when (provider) {
            LlmProvider.OPENAI -> OpenAiChatModel.builder()
                .apiKey(apiKey ?: error("API key required for OpenAI"))
                .baseUrl(url ?: "https://api.openai.com/v1")
                .modelName(modelName ?: "gpt-4o-mini")
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .responseFormat(ResponseFormat.JSON)
                .build()

            LlmProvider.AZURE_OPENAI -> AzureOpenAiChatModel.builder()
                .endpoint(url ?: error("Endpoint/URL required for Azure OpenAI"))
                .apiKey(apiKey ?: error("API key required for Azure OpenAI"))
                .deploymentName(modelName ?: error("Model/deployment name required for Azure OpenAI"))
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .responseFormat(ResponseFormat.JSON)
                .build()

            LlmProvider.DEEPSEEK -> OpenAiChatModel.builder()
                .apiKey(apiKey ?: error("API key required for DeepSeek"))
                .baseUrl(url ?: "https://api.deepseek.com")
                .modelName(modelName ?: "deepseek-v4-pro")
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .responseFormat(ResponseFormat.JSON)
                //.accumulateToolCallId(false) //needed for DeepSeek?
                .build()

            LlmProvider.OLLAMA -> OllamaChatModel.builder()
                .baseUrl(url ?: "http://localhost:11434")
                .modelName(modelName ?: "deepseek-r1:70b")
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .responseFormat(ResponseFormat.JSON)
                .build()
        }
    }



    fun createStreamingModel(
        provider: LlmProvider,
        apiKey: String? = null,
        url: String? = null,
        modelName: String? = null,
        timeoutSeconds: Long = 60,
        temperature: Double = 0.3,
    ): StreamingChatModel {

        if(timeoutSeconds < 0){
            throw IllegalArgumentException("Negative timeout: $timeoutSeconds")
        }
        if(temperature !in 0.0..2.0){
            throw IllegalArgumentException("Invalid temperature: $temperature")
        }

        /*
            unfortunately it does not seem these builders share any common superclass :(
            so most of the configurations need to be copy&paste
         */

        return when (provider) {
            LlmProvider.OPENAI -> OpenAiStreamingChatModel.builder()
                .apiKey(apiKey ?: error("API key required for OpenAI"))
                .baseUrl(url ?: "https://api.openai.com/v1")
                .modelName(modelName ?: "gpt-4o-mini")
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .responseFormat(ResponseFormat.JSON)
                .build()

            LlmProvider.AZURE_OPENAI -> AzureOpenAiStreamingChatModel.builder()
                .endpoint(url ?: error("Endpoint/URL required for Azure OpenAI"))
                .apiKey(apiKey ?: error("API key required for Azure OpenAI"))
                .deploymentName(modelName ?: error("Model/deployment name required for Azure OpenAI"))
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .responseFormat(ResponseFormat.JSON)
                .build()

            LlmProvider.DEEPSEEK -> OpenAiStreamingChatModel.builder()
                .apiKey(apiKey ?: error("API key required for DeepSeek"))
                .baseUrl(url ?: "https://api.deepseek.com")
                .modelName(modelName ?: "deepseek-v4-pro")
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .responseFormat(ResponseFormat.JSON)
                .accumulateToolCallId(false) //needed for DeepSeek?
                .build()

            LlmProvider.OLLAMA -> OllamaStreamingChatModel.builder()
                .baseUrl(url ?: "http://localhost:11434")
                .modelName(modelName ?: "deepseek-r1:70b")
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .responseFormat(ResponseFormat.JSON)
                .build()
        }
    }
}