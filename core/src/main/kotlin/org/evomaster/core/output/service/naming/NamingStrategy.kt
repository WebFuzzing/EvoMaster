package org.evomaster.core.output.service.naming

enum class NamingStrategy {

    NUMBERED
    ;

    fun isNumbered() = this.name.startsWith("numbered", true)
}
