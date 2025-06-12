package org.evomaster.core.languagemodel.data

class AnsweredPrompt (
    val prompt: Prompt,
    val answer: Any,
    val formattedResponse: Boolean = false
) {
}
