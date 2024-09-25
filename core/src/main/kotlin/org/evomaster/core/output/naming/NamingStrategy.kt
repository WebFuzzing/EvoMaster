package org.evomaster.core.output.naming

enum class NamingStrategy {

    NUMBERED
    ;

    fun isNumbered() = this.name.startsWith("numbered", true)
}
