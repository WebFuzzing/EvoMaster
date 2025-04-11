package org.evomaster.core.search.gene.regex

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.LoggerFactory

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
) : RxAtom, SimpleGene("\\$type") {

    companion object{
        private val log = LoggerFactory.getLogger(CharacterRangeRxGene::class.java)
    }

    var value: String = ""

    init {
        if (!listOf("w", "W", "d", "D", "s", "S").contains(type)) {
            throw IllegalArgumentException("Invalid type: $type")
        }
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return value.matches(Regex("\\$type"))
    }

    override fun copyContent(): Gene {
        val copy = CharacterClassEscapeRxGene(type)
        copy.value = this.value
        return copy
    }

    override fun setValueWithRawString(value: String) {
        this.value = value
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        val previous = value

        value = when(type){
            "d" -> randomness.nextDigitChar()
            "D" -> randomness.nextNonDigitChar()
            "w" -> randomness.nextWordChar()
            "W" -> randomness.nextNonWordChar()
            "s" -> randomness.nextSpaceChar()
            "S" -> randomness.nextNonSpaceChar()
            else ->
                //this should never happen due to check in init
                throw IllegalStateException("Type '\\$type' not supported yet")
        }.toString()

        if(tryToForceNewValue && previous == value){
            randomize(randomness, tryToForceNewValue)
        }
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        if (value=="") {
            // if standardMutation was invoked before calling to randomize
            // then we signal an exception
            throw IllegalStateException("Cannot apply mutation on an uninitialized gene")
        }

        if(type == "d"){
            value = ((value.toInt() + randomness.choose(listOf(1,-1)) + 10) % 10).toString()
        } else {
            randomize(randomness, true)
        }

        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
       return value
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if(other !is CharacterClassEscapeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        val current = this.value
        this.value = other.value
        if (!isLocallyValid()){
            this.value = current
            return false
        }

        return true
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is CharacterClassEscapeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        if (gene is CharacterClassEscapeRxGene){
            value = gene.value
            return true
        }
        LoggingUtil.uniqueWarn(log,"cannot bind CharacterClassEscapeRxGene with ${gene::class.java.simpleName}")
        return false
    }

}