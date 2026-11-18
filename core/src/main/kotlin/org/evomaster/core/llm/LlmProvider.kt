package org.evomaster.core.llm

enum class LlmProvider {

    OPENAI,
    DEEPSEEK,   // OpenAI-compatible
    ORCAROUTER, // OpenAI-compatible, https://api.orcarouter.ai/v1
    OLLAMA,
    AZURE_OPENAI,
    MOCK
}