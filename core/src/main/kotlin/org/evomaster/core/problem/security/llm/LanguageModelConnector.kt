package org.evomaster.core.problem.security.llm

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import java.net.HttpURLConnection
import java.net.URL
import javax.annotation.PostConstruct
import org.slf4j.LoggerFactory
import java.io.DataOutputStream

class LanguageModelConnector {

    companion object {
        private val log = LoggerFactory.getLogger(LanguageModelConnector::class.java)
    }

    @PostConstruct
    fun initialise() {
        log.debug("Initializing {}", LanguageModelConnector::class.simpleName)
    }

    @Inject
    private lateinit var config: EMConfig

    /**
     * To query the LLM.
     *
     * Note: If you are using Ollama as a server, please make sure to set the
     * CORS origin for Ollama on the host operating system.
     *
     * https://medium.com/dcoderai/how-to-handle-cors-settings-in-ollama-a-comprehensive-guide-ee2a5a1beef0
     */
    fun query(prompt: String): String? {
        if (prompt.isBlank()) {
            throw IllegalArgumentException("Prompt cannot be empty")
        }

        val languageModelURL = if (config.languageModelUrl == null) {
            URL("http://localhost:11434/api/generate")
        } else {
            URL(config.languageModelUrl)
        }

        val languageModelName = if (config.languageModelName == null) {
            "llama3.2:latest"
        } else {
            config.languageModelName
        }

        val objectMapper = ObjectMapper()
        val requestBody = objectMapper.writeValueAsString(LanguageModelRequestDto(
            prompt = prompt,
            stream = false,
            model = languageModelName.toString()
        ))

        val connection = languageModelURL.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        // TODO: set to avoid long running calls
        connection.connectTimeout = 4000
        connection.setRequestProperty("Content-Type", "application/json")
        connection.useCaches = false
        connection.doInput = true
        connection.doOutput = true

        val writer = DataOutputStream(connection.outputStream)
        writer.writeBytes(requestBody)
        writer.flush()
        writer.close()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            LoggingUtil.uniqueWarn(log, "Failed to connect to language model server")
            return null
        }

        val response = objectMapper.readValue(connection.inputStream, LanguageModelResponseDto::class.java)

        return response.response
    }
}
