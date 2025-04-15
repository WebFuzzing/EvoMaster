package org.evomaster.core.languagemodel

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.slf4j.LoggerFactory
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.annotation.PostConstruct

/**
 * A utility service designed to handle large language model server
 * related functions.
 *
 * Designed to work with Ollama (version 0.6.2).
 */
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

    private val objectMapper = ObjectMapper()


    /**
     * To query the large language server with a simple prompt.
     */
    fun query(prompt: String): String? {
        if (prompt.isBlank()) {
            throw IllegalArgumentException("Prompt cannot be empty")
        }

        val languageModelServerURL = getLanguageModelServerURL()

        val languageModelName = getLanguageModelName()

        val requestBody = objectMapper.writeValueAsString(
            OllamaRequestDto(
                prompt = prompt,
                stream = false,
                model = languageModelName.toString()
            )
        )

        val response = call(languageModelServerURL, requestBody)

        return response
    }


    /**
     * To query the large language model server with a structured output schema.
     * TODO: Could be useful in some cases.
     */
    fun queryStructured(prompt: String): String? {
        if (prompt.isBlank()) {
            throw IllegalArgumentException("Prompt cannot be empty")
        }

        TODO("Can be used to extract structured output.")
    }


    /**
     * Private method to make the call to the large language model server.
     *
     * Note: If you are using Ollama as a server, please make sure to set the
     * CORS origin for Ollama on the host operating system.
     *
     * Reference:
     * https://medium.com/dcoderai/how-to-handle-cors-settings-in-ollama-a-comprehensive-guide-ee2a5a1beef0
     */
    private fun call(languageModelServerURL: String, requestBody: String): String? {
        try {
            val connection = URL(languageModelServerURL).openConnection() as HttpURLConnection
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
                LoggingUtil.Companion.uniqueWarn(log, "Failed to connect to language model server")
                return null
            }

            // This is designed to use the non-stream outputs.
            // If stream is needed, consider implementing a
            // different method to handle stream outputs.
            val response = objectMapper.readValue(connection.inputStream, OllamaResponseDto::class.java)

            return response.response
        } catch (e: Exception) {
            LoggingUtil.Companion.uniqueWarn(log, "Failed to connect to language model server: ${e.message}")

            return null
        }
    }


    /**
     * Private method, returns the large language model server URL from
     * EMConfig, otherwise returns the possible default local URL.
     */
    private fun getLanguageModelServerURL(): String {
        val languageModelServerURL = if (config.languageModelServerURL == null) {
            "http://localhost:11434/api/generate"
        } else {
            config.languageModelServerURL
        }

        if (languageModelServerURL.isNullOrEmpty()) {
            throw IllegalArgumentException("Language model URL cannot be empty")
        }

        return languageModelServerURL
    }

    /**
     * Private method, returns the large language model server name from
     * EMConfig, otherwise return llama3.2:latest.
     *
     * TODO: Failsafe model name yet to be decided
     */
    private fun getLanguageModelName(): String {
        val languageModelName = if (config.languageModelName == null) {
            "llama3.2:latest"
        } else {
            config.languageModelName
        }

        if (languageModelName.isNullOrEmpty()) {
            throw IllegalArgumentException("Language model name cannot be empty")
        }

        return languageModelName
    }
}
