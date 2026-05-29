package org.evomaster.core.llm

import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import java.util.concurrent.CompletableFuture


object LlmSupport {

    fun chatAsync(model: StreamingChatModel, userMessage: String): CompletableFuture<String> {
        val future = CompletableFuture<String>()

        model.chat(userMessage, object : StreamingChatResponseHandler {
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


    fun createModel(
        provider: LlmProvider,
        apiKey: String? = null,
        url: String? = null,
        modelName: String? = null,
        timeoutSeconds: Long = 60,
        temperature: Double = 0.3,
    ): StreamingChatModel {

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
                .build()

            LlmProvider.AZURE_OPENAI -> AzureOpenAiStreamingChatModel.builder()
                .endpoint(url ?: error("Endpoint/URL required for Azure OpenAI"))
                .apiKey(apiKey ?: error("API key required for Azure OpenAI"))
                .deploymentName(modelName ?: error("Model/deployment name required for Azure OpenAI"))
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .build()

            LlmProvider.DEEPSEEK -> OpenAiStreamingChatModel.builder()
                .apiKey(apiKey ?: error("API key required for DeepSeek"))
                .baseUrl(url ?: "https://api.deepseek.com/v1")
                .modelName(modelName ?: "deepseek-chat")
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .build()

            LlmProvider.OLLAMA -> OllamaStreamingChatModel.builder()
                .baseUrl(url ?: "http://localhost:11434")
                .modelName(modelName ?: "deepseek-r1:70b")
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .build()
        }
    }
}