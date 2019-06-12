package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness


class CharacterRangeRxGene(
        val negated: Boolean,
        val ranges: List<Pair<Char,Char>>
) : RxAtom("."){

    var value : Char = ' '

    init {
        //TODO this will need to be supported
        if(negated){
            throw IllegalArgumentException("Negated ranges are not supported yet")
        }
    }


    override fun copy(): Gene {
        val copy = CharacterRangeRxGene(negated, ranges)
        copy.value = this.value
        return copy
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        /*
            TODO current is very simple, biased implementation.
            Should rather have uniform sampling among all valid chars
         */
        val range = randomness.choose(ranges)

        value = randomness.nextChar(range.first, range.second)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        /*
            TODO should \ be handled specially?
            In any case, would have same handling as AnyCharacterRxGene
         */
        return value.toString()
    }

    override fun copyValueFrom(other: Gene) {
        if(other !is CharacterRangeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is CharacterRangeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }


}