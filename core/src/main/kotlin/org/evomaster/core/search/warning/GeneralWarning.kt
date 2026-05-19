package org.evomaster.core.search.warning

data class GeneralWarning(

    /**
     * Label to specify the type of warning, e.g., if related to the schema or
     * fuzzer misconfiguration.
     */
    val category: WarningCategory,

    /**
     * The textual content of the warning message.
     */
    val message: String
)
