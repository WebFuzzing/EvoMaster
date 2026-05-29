package org.evomaster.core.output.naming

enum class NamingStrategy {

    NUMBERED,
    ACTION,
    LLM
    ;

    fun isNumbered() = this == NUMBERED

    fun isAction() = this == ACTION

    fun isLLM() = this == LLM
}
