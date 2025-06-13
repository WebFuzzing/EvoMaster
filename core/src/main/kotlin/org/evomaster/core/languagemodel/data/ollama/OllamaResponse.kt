package org.evomaster.core.languagemodel.data.ollama

/**
 * DTO to represent the Ollama response schema.
 */
class OllamaResponse {

    /**
     * Used model name
     */
    val model: String = ""

    val created_at: String = ""

    /**
     * Contains the response string for non-stream output
     */
    val response: String = ""

    val done: Boolean = false

    val done_reason: String = ""

    val context: List<Int> = emptyList()

    val total_duration: Long = 0

    val load_duration: Long = 0

    val prompt_eval_count: Long = 0

    val prompt_eval_duration: Long = 0

    val eval_count: Long = 0

    val eval_duration: Long = 0
}
