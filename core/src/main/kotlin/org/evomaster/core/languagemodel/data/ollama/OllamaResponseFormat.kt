package org.evomaster.core.languagemodel.data.ollama

class OllamaResponseFormat (
    val type: String,

    val properties: Map<String, OllamaResponseProperty>,

    val required: List<String>,
) {
}
