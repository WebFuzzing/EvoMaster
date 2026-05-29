package org.evomaster.core.llm

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class LlmSupportTest {


    @Test
    fun testBasePrompt(){
        //small model, 1GB
        val model = LlmSupport.createModel(LlmProvider.OLLAMA, modelName = "deepseek-r1:1.5b", temperature = 0.0)

        val prompt = """
            Is A  the first letter in the English alphabet? Answer this question with either a YES or a NO.
            Do not add anything else in your response.
            """.trimIndent()

        val included = "YES"
        val excluded = "NO"

        val response = LlmSupport.chatAsync(model, prompt).get()
        assertTrue(response.contains(included), "Wrong response: $response")
        assertTrue(!response.contains(excluded), "Wrong response: $response")
    }
}