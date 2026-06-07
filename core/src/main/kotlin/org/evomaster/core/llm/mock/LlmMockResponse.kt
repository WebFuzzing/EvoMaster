package org.evomaster.core.llm.mock

class LlmMockResponse(

    /**
     * The response producer that will be returned by the LLM, given an input text from user
     */
    val responseProducer: (String) -> String,

    /**
     * Lambda that, given as input a request from the client, decides whether this response should be returned
     */
    val matcher: (String) -> Boolean
)
