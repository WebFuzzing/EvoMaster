package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy

/*
\w	Find a word character
\W	Find a non-word character
\d	Find a digit
\D	Find a non-digit character
\s	Find a whitespace character
\S	Find a non-whitespace character
 */
class CharacterClassEscapeRxGene(
        val type: String
) : RxAtom("\\$type") {


    var value: String = ""

    init {
        if (!listOf("w", "W", "d", "D", "s", "S").contains(type)) {
            throw IllegalArgumentException("Invalid type: $type")
        }
    }

    override fun copy(): Gene {
        val copy = CharacterClassEscapeRxGene(type)
        copy.value = this.value
        return copy
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        value = when(type){
            "d" -> randomness.nextInt(0,9).toString()
            "w" -> randomness.nextLetter().toString()
            //TODO all cases
            else -> throw IllegalStateException("Type '\\$type' not supported yet")
        }
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): Boolean {
        if (value=="") {
            // if standardMutation was invoked before calling to randomize
            // then we signal an exception
            throw IllegalStateException("Cannot apply mutation on an uninitalized gene")
        }

        value = when(type){
            "d" -> ((value.toInt() + randomness.choose(listOf(1,-1)) + 10) % 10).toString()
            //TODO all cases
            else -> throw IllegalStateException("Type '\\${type}' not supported yet")
        }

        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
       return value
    }

    override fun copyValueFrom(other: Gene) {
        if(other !is CharacterClassEscapeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is CharacterClassEscapeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

}