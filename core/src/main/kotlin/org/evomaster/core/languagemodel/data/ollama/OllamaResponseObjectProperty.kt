package org.evomaster.core.languagemodel.data.ollama

class OllamaResponseObjectProperty(
    override val type: String,
    val properties: Map<String, OllamaResponseProperty>
) : OllamaResponseProperty(type) {

}
