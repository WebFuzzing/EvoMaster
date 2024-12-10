package org.evomaster.core.search.gene.datetime

enum class FormatForDatesAndTimes {

    // YYYY-MM-DDTHH:MM:SS
    ISO_LOCAL,


    //https://datatracker.ietf.org/doc/html/rfc3339#section-5.6
    // // YYYY-MM-DDTHH:MM:SS[.mmm](Z|+-hh)
    RFC3339,


    // YYYY-MM-DD HH:MM:SS
    // Note the missing T. used for example in SQL
    DATETIME
}