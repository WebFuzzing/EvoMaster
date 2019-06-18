package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.Randomness

/**
 * A wrapper around a Gene to represent a column in a SQL database
 * where such column is a Primary Key.
 * This is important to check Foreign Keys referencing it.
 */
class SqlPrimaryKeyGene(name: String,
                        val tableName: String,
                        val gene: Gene,
                        /**
                         * Important for the Foreign Keys referencing it.
                         * Cannot be negative
                         */
                        val uniqueId: Long
) : Gene(name) {

    init {
        if (uniqueId < 0) {
            throw IllegalArgumentException("Negative unique id")
        }
    }

    override fun copy() = SqlPrimaryKeyGene(name, tableName, gene.copy(), uniqueId)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        gene.randomize(randomness, false, allGenes)
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlPrimaryKeyGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.gene.copyValueFrom(other.gene)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlPrimaryKeyGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.gene.containsSameValueAs(other.gene)
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        return gene.getValueAsPrintableString(previousGenes, mode, targetFormat)
    }

    override fun getValueAsRawString(): String {
        return gene.getValueAsRawString()
    }

    override fun getVariableName() = gene.getVariableName()

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(gene.flatView(excludePredicate))
    }

    override fun isMutable() = gene.isMutable()

    override fun isPrintable() = gene.isPrintable()


    fun isReferenceToNonPrintable(previousGenes: List<Gene>): Boolean {
        if (gene !is SqlForeignKeyGene) {
            return false
        }

        return gene.isReferenceToNonPrintable(previousGenes)
    }
}