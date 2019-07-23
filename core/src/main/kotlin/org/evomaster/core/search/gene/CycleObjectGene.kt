package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
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
class CycleObjectGene(name: String) : ObjectGene(name, listOf()) {

    override fun isMutable() = false

    override fun copy(): Gene = CycleObjectGene(name)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        //nothing to do
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        randomize(randomness, true, allGenes)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        throw IllegalStateException("CycleObjectGene has no value")
    }

}