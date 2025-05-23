package org.evomaster.core.languagemodel.data

import java.util.UUID

class AnsweredPrompt (
    val promptID: UUID,
    val prompt: String,
    val answer: String
) {
}
