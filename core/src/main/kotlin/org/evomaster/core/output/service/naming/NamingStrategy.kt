package org.evomaster.core.output.service.naming

enum class NamingStrategy {

    DEFAULT,
    NUMBERED
    ;

    fun isNumbered() = this.name.startsWith("numbered", true)
}
