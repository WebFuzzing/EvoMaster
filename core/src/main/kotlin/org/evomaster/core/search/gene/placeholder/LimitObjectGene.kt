package org.evomaster.core.search.gene.placeholder

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness

/**
 * This gene is mainly created for GraphQL.
 * It is used as a placeholder when a certain limit is reached
 */

class LimitObjectGene(name: String) : SimpleGene(name) {

    override fun isMutable() = false

    override fun copyContent(): Gene = LimitObjectGene(name)

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        //nothing to do
    }

    override fun setValueWithRawString(value: String) {
        throw IllegalStateException("cannot set value with string ($value) for LimitObjectGene")
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        throw IllegalStateException("LimitObjectGene has no value")
    }


    override fun isPrintable(): Boolean {
        return false
    }

    override fun copyValueFrom(other: Gene): Boolean {
        return true
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        return other is LimitObjectGene
    }

    override fun setValueBasedOn(gene: Gene): Boolean {
        return false
    }
}