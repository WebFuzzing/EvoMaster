package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness


class AnyCharacterRxGene : RxAtom("."){

    var value: Char = 'a'

    override fun copy(): Gene {
        val copy = AnyCharacterRxGene()
        copy.value = this.value
        return copy
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        //TODO properly... this is just a tmp hack
        value = randomness.nextWordChar()
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        randomize(randomness, true, allGenes)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        /*
            TODO should \ be handled specially?
         */
        return value.toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is AnyCharacterRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is AnyCharacterRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }
}