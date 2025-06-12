package org.evomaster.core.languagemodel.data

class AnsweredPrompt (
    val prompt: Prompt,
    val answer: String,
    val formattedResponse: Boolean = false
) {
}
