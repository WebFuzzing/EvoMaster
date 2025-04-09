package org.evomaster.core.languagemodel

/**
 * DTO to represent the Ollama response schema.
 */
class OllamaResponseDto {

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

    val total_duration: Int = 0

    val load_duration: Int = 0

    val prompt_eval_count: Int = 0

    val prompt_eval_duration: Int = 0

    val eval_count: Int = 0

    val eval_duration: Int = 0
}
