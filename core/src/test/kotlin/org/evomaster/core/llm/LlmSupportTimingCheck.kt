package org.evomaster.core.llm

import org.evomaster.core.utils.TimeUtils

class LlmSupportTimingCheck {


    companion object {

        val userMessage = """
            Your input is
                [targetLanguage]:Java
                [remainingNameChars]: 112
                [generatedNames]: []
                [testLines]: /**
    * Calls:
    * (400) GET:/api/pat/{txt}
    * Found 1 potential fault of type-code 200
    */
    @Test(timeout = 60000)
    public void test() throws Exception {

        // Fault200. Schema Violation: Received A Response From API With A Structure/Data That Is Not Matching Its Schema. Type: validation.response.status.unknown Response status 400 not defined for path '/api/pat/{txt}'.
        given().accept("*/*")
                .header("x-EMextraHeader123", "")
                .get(baseUrlOfSut + "/api/pat/4%5C")
                .then()
                .statusCode(400)
                .assertThat()
                .body(isEmptyOrNullString());
    }
        """.trimIndent()


        @JvmStatic
        fun main(args: Array<String>) {

            val temperature = 0.6

//            val model = LlmSupport.createModel(LlmProvider.OLLAMA, modelName = "deepseek-r1:1.5b", temperature = temperature)
//            val model = LlmSupport.createModel(LlmProvider.OLLAMA, modelName = "deepseek-r1:8b", temperature = temperature)
            val model = LlmSupport.createModel(LlmProvider.OLLAMA, modelName = "qwen3.8:27b", temperature = temperature)

            val systemMessage = Prompts.NEW_TEST_CASE_NAME

            TimeUtils.measureTimeMillis(
                {ms, res -> println(res); println("\n\n\nTime: ${ms}ms")},
                {LlmSupport.chat(model, systemMessage, userMessage)}
            )

        }
    }
}