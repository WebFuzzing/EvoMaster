package org.evomaster.core.languagemodel

class PromptDto (
    val id: String,

    val prompt: String
) {

    var answer: String? = null

    fun hasAnswer(): Boolean {
        return this.answer != null
    }
}
