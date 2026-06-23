package org.evomaster.core.search.gene.datetime

enum class FormatForDatesAndTimes(
    val pattern: String
) {

    // ISO 8601
    ISO_LOCAL("YYYY-MM-DDTHH:MM:SS"),

    //https://datatracker.ietf.org/doc/html/rfc3339#section-5.6
    RFC3339("YYYY-MM-DDTHH:MM:SS[.mmm](Z|+-hh)"),

    // Note the missing T. used for example in SQL
    DATETIME("YYYY-MM-DD HH:MM:SS")
}