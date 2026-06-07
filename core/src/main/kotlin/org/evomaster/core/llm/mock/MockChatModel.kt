package org.evomaster.core.llm.mock

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.ChatRequestOptions
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse

class MockChatModel : ChatModel {

    companion object {

        /*
            WARNING
            Global mutable state... but only used for testing.
            But out of the box would prevent parallel tests in same process, which we don't anyway,
            so shouldn't be a big deal
         */
        private val responses : MutableList<LlmMockResponse> = mutableListOf()

        /**
         * Clear the cache of mocked responses.
         * Important to avoid dependencies among tests.
         * Recall this is using mutable static state.
         */
        fun reset(){
            responses.clear()
        }

        /**
         * Specify the [response] that will be returned by the LLM,
         * based on [matcher] that, given as input a request from the client,
         * decides whether this response should be returned
         */
        fun mockResponse(response: String, matcher: (String) -> Boolean){
            responses.add(LlmMockResponse(response, matcher))
        }
    }

    /**
     * Main method that needs to be mocked.
     * All other methods seem to call this one.
     */
    override fun chat(chatRequest: ChatRequest, options: ChatRequestOptions) : ChatResponse {

        val text = chatRequest.messages().joinToString { it.toString() }

        val res = responses.find { mock -> mock.matcher(text) }?.response
            ?: "Hei! You forgot to setup the mock for me"

        return ChatResponse.builder().aiMessage(AiMessage.from(res)).build()
    }
}