package org.evomaster.core.search.gene.sql

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.sql.SqlStrings.removeEnclosedQuotationMarks

class SqlMultiRangeGene<T>(
        name: String,
        template: SqlRangeGene<T>,
        minSize: Int? = null,
        maxSize: Int? = null,
        elements: MutableList<SqlRangeGene<T>> = mutableListOf()
) : ArrayGene<SqlRangeGene<T>>(name, template, maxSize, minSize, elements) where T : ComparableGene {


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return "\"{" +
                elements.map { g ->
                    removeEnclosedQuotationMarks(g.getValueAsPrintableString(previousGenes, mode, targetFormat))
                }.joinToString(", ") +
                "}\""
    }

}