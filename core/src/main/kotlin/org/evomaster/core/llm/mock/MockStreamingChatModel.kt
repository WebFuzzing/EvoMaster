package org.evomaster.core.llm.mock

import dev.langchain4j.model.chat.ChatRequestOptions
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler

/**
 * WARNING:
 * we wrote the code for it, but in the end we did not use StreamingChatModel.
 * TODO If one day we do, then we need to update this class if we want to have tests using it.
 */
class MockStreamingChatModel : StreamingChatModel {

    override fun chat(request: ChatRequest?, options: ChatRequestOptions?, handler: StreamingChatResponseHandler?){

        throw NotImplementedError()
    }
}