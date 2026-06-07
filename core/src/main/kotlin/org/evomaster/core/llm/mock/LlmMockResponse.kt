package org.evomaster.core.llm.mock

class LlmMockResponse(

    /**
     * The response that will be returned by the LLM
     */
    val response: String,

    /**
     * Lambda that, given as input a request from the client, decides whether this response should be returned
     */
    val matcher: (String) -> Boolean
)
