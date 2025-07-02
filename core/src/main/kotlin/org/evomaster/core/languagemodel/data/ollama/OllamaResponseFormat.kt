package org.evomaster.core.languagemodel.data.ollama

/**
 * Represents Ollama custom response format request.
 */
class OllamaResponseFormat (
    val type: String,

    /**
     * Represents the response format properties.
     * Key holds the name of the properties and value holds the property
     * type as [OllamaResponseProperty].
     * For array type use [OllamaResponseArrayProperty].
     */
    val properties: Map<String, OllamaResponseProperty>,

    /**
     * List to hold required fields on the response.
     */
    val required: List<String>,
) {
}
