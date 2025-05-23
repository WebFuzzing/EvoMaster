package org.evomaster.core.languagemodel.data

import java.util.UUID

class AnsweredPrompt (
    val prompt: String,
    val answer: String,
    val promptID: UUID? = null
) {
}
