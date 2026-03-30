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
import org.evomaster.core.utils.CharacterRange
import org.evomaster.core.utils.MultiCharacterRange
import org.slf4j.LoggerFactory

class CharacterRangeRxGene private constructor(
    /**
     * this represents the valid ranges for a character class, removing overlaps and applying negation
     */
    val validRanges: MultiCharacterRange
) : RxAtom, SimpleGene("."){

    constructor(negated: Boolean, ranges: List<CharacterRange>) : this(MultiCharacterRange(negated, ranges))

    companion object{
        private val log = LoggerFactory.getLogger(CharacterRangeRxGene::class.java)
    }

    var value : Char = validRanges[0].start

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return validRanges.any { value in it }
    }

    override fun isMutable(): Boolean {
        return validRanges.size > 1 || validRanges[0].size > 1
    }

    override fun copyContent(): Gene {
        val copy = CharacterRangeRxGene(validRanges)
        copy.value = this.value
        copy.name = this.name //in case name is changed from its default
        return copy
    }

    override fun setValueWithRawString(value: String) {
        // need to check
        val c = value.toCharArray().firstOrNull()
        if (c!= null)
            this.value = c
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        val total = validRanges.sumOf { it.size }
        val sampledValue = randomness.nextInt(total)
        var currentRangeMinValue = 0
        for (r in validRanges) {
            val currentRangeMaxValue = currentRangeMinValue + r.size
            if (sampledValue < currentRangeMaxValue) {
                val codePoint = r.start.code + (sampledValue - currentRangeMinValue)
                // is it necessary to log this?
                log.trace("using Int {} as character selector for character class, resulting in code point: {}, which is: {}", sampledValue, codePoint, codePoint.toChar())
                value = codePoint.toChar()
                return
            }
            currentRangeMinValue = currentRangeMaxValue
        }
        assert(false) // internalRanges being empty should never happen
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        var t = 0
        for(i in 0 until validRanges.size){
            val p = validRanges[i]
            if(value in p){
                t = i
                break
            }
        }

        val delta = randomness.choose(listOf(1,-1))

        if(value + delta > validRanges[t].end){
            /*
                going over current max range. check next range
                and take its minimum
             */
            val next = (t+1) % validRanges.size
            value = validRanges[next].start

        } else if(value + delta < validRanges[t].start){

            val previous = (t - 1 + validRanges.size) % validRanges.size
            value = validRanges[previous].end

        } else {
            value += delta
        }

        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        /*
            TODO should \ be handled specially?
            In any case, would have same handling as AnyCharacterRxGene
         */
        return value.toString()
    }


    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is CharacterRangeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {

        val gene = other.getPhenotype()

        if(gene is CharacterRangeRxGene){
            value = gene.value
            return true
        }
        LoggingUtil.uniqueWarn(log,"cannot bind CharacterClassEscapeRxGene with ${gene::class.java.simpleName}")

        return false
    }
}
