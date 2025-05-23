package org.evomaster.core.languagemodel.data

import java.util.UUID

class Prompt (
    val id: UUID,

    val prompt: String
) {

    var answer: AnsweredPrompt? = null

    fun hasAnswer(): Boolean {
        return this.answer != null
    }
}
