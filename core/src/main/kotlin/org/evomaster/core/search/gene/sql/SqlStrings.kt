package org.evomaster.core.search.gene.sql

object SqlStrings {
    const val SINGLE_APOSTROPHE_PLACEHOLDER = "SINGLE_APOSTROPHE_PLACEHOLDER"

    private val QUOTATION_MARK = "\""

    fun removeEnclosedQuotationMarks(str: String): String {
        return if (str.startsWith(QUOTATION_MARK) && str.endsWith(QUOTATION_MARK)) {
            str.subSequence(1, str.length - 1).toString()
        } else {
            str
        }
    }

    private fun encloseWithSingleApostrophePlaceHolder(str: String) = SINGLE_APOSTROPHE_PLACEHOLDER + str + SINGLE_APOSTROPHE_PLACEHOLDER

    fun replaceEnclosedQuotationMarksWithSingleApostrophePlaceHolder(str: String): String {
        return if (str.startsWith(QUOTATION_MARK) && str.endsWith(QUOTATION_MARK)) {
            encloseWithSingleApostrophePlaceHolder(removeEnclosedQuotationMarks(str))
        } else {
            str
        }
    }
}