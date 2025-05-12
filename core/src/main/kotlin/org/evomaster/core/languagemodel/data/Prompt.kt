package org.evomaster.core.languagemodel.data

class Prompt (
    val id: String,

    val prompt: String
) {

    var answer: String? = null

    fun hasAnswer(): Boolean {
        return this.answer != null
    }
}
