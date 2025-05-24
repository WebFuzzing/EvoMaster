package org.evomaster.core.languagemodel.data.ollama

/**
 * DTO to represent the Ollama request schema.
 */
class OllamaRequest (
    val model: String,

    /**
     * Contains the string of the prompt for the language model.
     */
    val prompt: String,

    /**
     * False will return the response as a single object; meanwhile,
     * True will respond a stream of objects.
     */
    val stream: Boolean = false
) {

}
