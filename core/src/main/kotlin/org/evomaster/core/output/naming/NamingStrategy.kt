package org.evomaster.core.output.naming

enum class NamingStrategy {

    NUMBERED,
    ACTION
    ;

    fun isNumbered() = this.name.startsWith("numbered", true)

    fun isAction() = this.name.startsWith("action", true)
}
