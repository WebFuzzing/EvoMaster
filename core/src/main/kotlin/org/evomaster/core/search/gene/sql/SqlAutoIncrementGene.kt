package org.evomaster.core.search.gene.sql

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.service.Randomness


class SqlAutoIncrementGene(name: String) : SimpleGene(name) {

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene {
        return SqlAutoIncrementGene(name)
    }

    override fun setValueWithRawString(value: String) {
        throw IllegalStateException("cannot set value with string ($value) for ${this.javaClass.simpleName}")
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        throw IllegalStateException("AutoIncrement fields are not part of the search")
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        throw IllegalStateException("AutoIncrement fields should never be printed")
    }

    /**
     * TODO Shouldn't this method throw an IllegalStateException ?
     *
     * Man: need to check with Andrea, copyValueFrom of [ImmutableDataHolderGene] throw an exception
     */
    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlAutoIncrementGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        //do nothing
        return true
    }

    /**
     * Since each object instance of SqlAutoIncrementGene represents a different
     * value for that particular column, we check for object identity to check
     * that two genes have the same value
     */
    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlAutoIncrementGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this === other
    }

    override fun isMutable() = false

    override fun isPrintable() = false

    override fun mutationWeight(): Double = 0.0


    override fun setValueBasedOn(gene: Gene): Boolean {
        // do nothing, cannot bind with others
        return true
    }
}