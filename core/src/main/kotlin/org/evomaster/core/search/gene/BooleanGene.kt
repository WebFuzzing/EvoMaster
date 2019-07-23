package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness


class BooleanGene(name: String, var value: Boolean = true) : Gene(name) {


    override fun copy(): Gene {
        return BooleanGene(name, value)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        val k: Boolean = if (forceNewValue) {
            !value
        } else {
            randomness.nextBoolean()
        }

        value = k
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        value = ! value
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        return value.toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is BooleanGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is BooleanGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }
}