package org.evomaster.core.search.gene.placeholder

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness

/**
 * It might happen that object A has reference to B,
 * and B has reference to A', where A' might or might
 * not be equal to A.
 * In this case, we cannot represent A'.
 *
 * TODO need to handle cases when some of those are
 * marked with "required"
 *
 * Note that for [CycleObjectGene], its [refType] is null
 */
class CycleObjectGene(name: String) : SimpleGene(name) {

    override fun isMutable() = false

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }
    override fun copyContent(): Gene = CycleObjectGene(name)

    override fun setValueWithRawString(value: String) {
        throw IllegalStateException("cannot set value with string ($value) for CycleObjectGene")
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        //nothing to do
        //TODO should rather throw exception?
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        throw IllegalStateException("CycleObjectGene has no value")
    }

    override fun copyValueFrom(other: Gene): Boolean {
        // do nothing
        return true
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        return other is CycleObjectGene
    }

    override fun setValueBasedOn(gene: Gene): Boolean {
        return false
    }


    override fun isPrintable(): Boolean {
        return false
    }
}