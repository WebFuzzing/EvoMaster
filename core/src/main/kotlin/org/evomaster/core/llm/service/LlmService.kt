package org.evomaster.core.llm.service

import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import org.evomaster.core.EMConfig
import org.evomaster.core.config.ConfigProblemException
import org.evomaster.core.llm.LlmSupport
import java.util.concurrent.CompletableFuture
import javax.annotation.PostConstruct
import javax.inject.Inject

class LlmService {

    @Inject
    private lateinit var config: EMConfig

    private lateinit var model: StreamingChatModel

    @PostConstruct
    private fun initService() {

        if(!config.llm){
            //nothing to setup
            return
        }

        try {
            model = LlmSupport.createModel(
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
    }

    private fun checkUsingLLM(){
        if(!config.llm){
            throw IllegalStateException("LLM is not in use")
        }
    }

    fun chatAsync(userMessage: String): CompletableFuture<String>{
        checkUsingLLM()

        return LlmSupport.chatAsync(model, userMessage)
    }

    fun chat(userMessage: String) : String{
        return chatAsync(userMessage).get()
    }
}