package org.evomaster.core.languagemodel.data.ollama

class OllamaResponseArrayProperty(
    override val type: String,
    val items: OllamaResponseProperty? = null,
) : OllamaResponseProperty(type) {
}
