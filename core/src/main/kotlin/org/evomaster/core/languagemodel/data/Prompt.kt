package org.evomaster.core.languagemodel.data

import java.util.UUID

class Prompt (
    val id: UUID,

    val prompt: String
) {

    var answer: String? = null

    fun hasAnswer(): Boolean {
        return this.answer != null
    }
}
