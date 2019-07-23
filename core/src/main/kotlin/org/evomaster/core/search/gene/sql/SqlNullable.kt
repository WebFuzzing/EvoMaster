package org.evomaster.core.search.gene.sql

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import java.lang.IllegalStateException


class SqlNullable(name: String,
                  val gene: Gene,
                  var isPresent: Boolean = true
) : SqlWrapperGene(name) {

    init{
        if(gene is SqlWrapperGene && gene.getForeignKey() != null){
            throw IllegalStateException("SqlNullable should not contain a FK, " +
                    "as its nullability is handled directly in SqlForeignKeyGene")
        }
    }

    override fun getForeignKey(): SqlForeignKeyGene? {
        return null
    }

    override fun copy(): Gene {
        return SqlNullable(name, gene.copy(), isPresent)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        isPresent = if (!isPresent && forceNewValue)
            true
        else
            randomness.nextBoolean(0.1)

        gene.randomize(randomness, forceNewValue, allGenes)
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        if(! isPresent){
            isPresent = true
        } else if(randomness.nextBoolean(0.1)){
            isPresent = false
        } else {
            gene.standardMutation(randomness, apc, allGenes)
        }
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {

        if (!isPresent) {
            return "NULL"
        }

        return gene.getValueAsPrintableString(previousGenes, mode, targetFormat)
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlNullable) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.isPresent = other.isPresent
        this.gene.copyValueFrom(other.gene)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlNullable) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.isPresent == other.isPresent &&
                this.gene.containsSameValueAs(other.gene)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(gene.flatView(excludePredicate))
    }
}