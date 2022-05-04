package org.evomaster.core.search.gene.sql

object SqlStrings {
    const val SINGLE_APOSTROPHE_PLACEHOLDER = "SINGLE_APOSTROPHE_PLACEHOLDER"

    private val QUOTATION_MARK = "\""

    fun replaceEnclosedQuotationMarks(str: String): String {
        return if (str.startsWith(QUOTATION_MARK) && str.endsWith(QUOTATION_MARK)) {
            SINGLE_APOSTROPHE_PLACEHOLDER + str.subSequence(1, str.length - 1) + SINGLE_APOSTROPHE_PLACEHOLDER
        } else {
            str
        }
    }
}