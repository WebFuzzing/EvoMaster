package org.evomaster.core.llm

import org.evomaster.core.llm.mock.MockChatModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LlmSupportMockTest {

    @Test
    fun testBasePrompt(){
        val model = LlmSupport.createModel(LlmProvider.MOCK)

        val firstPrompt = """
            Is A  the first letter in the English alphabet? Answer this question with either a "YES" or a "NO".
            Do not add anything else in your response.
            """.trimIndent()

        val secondPrompt = """
            Is Sweden the capital of Norway? 
        """.trimIndent()

        MockChatModel.reset()
        MockChatModel.mockResponse("YES"){ it.contains("English")}
        MockChatModel.mockResponse("NO"){ it.contains("Norway")}


        assertEquals("YES", LlmSupport.chat(model, firstPrompt))
        assertEquals("NO", LlmSupport.chat(model, secondPrompt))
    }
}