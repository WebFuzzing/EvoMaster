package org.evomaster.core.problem.security.llm

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.core.EMConfig
import java.net.HttpURLConnection
import java.net.URL

class LanguageModelConnector {

    @Inject
    private lateinit var config: EMConfig

    /**
     * String contains LLM name corresponding to the
     * available models in Ollama.
     */
    private val langaugeModel: String = "llama2-uncensored"


    /**
     * To query the LLM.
     */
    fun query(prompt: String): String {
        if (prompt.isBlank()) {
            throw IllegalArgumentException("Prompt cannot be empty")
        }

        val languageModelURL = if (config.languageModelUrl == null) {
            URL("http://localhost:11434/api/generate")
        } else {
            URL(config.languageModelUrl)
        }

        val objectMapper = ObjectMapper()
        val requestBody = objectMapper.writeValueAsString(LanguageModelRequestDto(
            prompt = prompt,
            stream = false,
            model = langaugeModel
        ))

        val connection = languageModelURL.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 3000
        connection.setRequestProperty("accept", "application/json")

        val response = objectMapper.readValue(connection.inputStream, LanguageModelResponseDto::class.java)

        return response.response
    }
}
