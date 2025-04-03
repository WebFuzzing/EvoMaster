package org.evomaster.core.problem.security.llm

/**
 * DTO to represent the Ollama request schema.
 */
class LanguageModelRequestDto (
    val model: String,
    val prompt: String,
    val stream: Boolean
) {

}
