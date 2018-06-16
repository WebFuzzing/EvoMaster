package org.evomaster.core.search.gene

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
                    )
    : Gene(name) {

    init {
        if(uniqueId < 0){
            throw IllegalArgumentException("Negative unique id")
        }
    }

    override fun copy() = SqlPrimaryKeyGene(name, tableName, gene.copy(), uniqueId)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {
        gene.randomize(randomness, false)
    }

    override fun copyValueFrom(other: Gene) {
        if(other !is SqlPrimaryKeyGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.gene.copyValueFrom(other.gene)
    }

    override fun getValueAsPrintableString() : String{
        return gene.getValueAsPrintableString()
    }

    override fun getValueAsRawString() : String {
        return gene.getValueAsRawString()
    }

    override fun getVariableName() = gene.getVariableName()

    override fun flatView(): List<Gene>{
        return listOf(this).plus(gene.flatView())
    }

    override fun isMutable() = gene.isMutable()

    override fun isPrintable() = gene.isPrintable()

}