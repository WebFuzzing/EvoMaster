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
import org.evomaster.core.utils.RegexFlags
import org.slf4j.LoggerFactory

class CharacterRangeRxGene private constructor(
    /**
     * this represents the valid ranges for a character class, removing overlaps and applying negation
     */
    val validRanges: MultiCharacterRange,
    val flags: RegexFlags
) : RxAtom, SimpleGene("."){

    constructor(negated: Boolean, ranges: List<CharacterRange>, flags: RegexFlags = RegexFlags())
            : this(MultiCharacterRange(negated, ranges), flags)

    companion object{
        private val log = LoggerFactory.getLogger(CharacterRangeRxGene::class.java)
    }

    var value : Char = validRanges[0].start

    /**
     * Whether to output the character in uppercase.
     * Only meaningful when flags.caseInsensitive is true.
     */
    var useUpperCase: Boolean = false

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return validRanges.any {
            value in it ||
                    // to check validity we also have to take into account case insensitivity
                    ( flags.isCaseable(value) &&
                            ( value.lowercaseChar() in it || value.uppercaseChar() in it )
                    )
        }
    }

    override fun isMutable(): Boolean {
        // check if there is more than one character or if the character is caseable
        return validRanges.charCount > 1 || flags.isCaseable(value)
    }

    override fun copyContent(): Gene {
        val copy = CharacterRangeRxGene(validRanges, flags)
        copy.value = this.value
        copy.name = this.name //in case name is changed from its default
        copy.useUpperCase = this.useUpperCase
        return copy
    }

    override fun setValueWithRawString(value: String) {
        // need to check
        val c = value.toCharArray().firstOrNull()
        if (c!= null)
            this.value = c
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        val previous = value
        val previousUpper = useUpperCase

        value = validRanges.sample(randomness)
        useUpperCase = if (flags.isCaseable(value)) {
            randomness.nextBoolean()
        } else {
            false
        }

        if(tryToForceNewValue && previous == value && previousUpper == useUpperCase){
            randomize(randomness, tryToForceNewValue)
        }
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

        useUpperCase = if (flags.isCaseable(value)) {
            randomness.nextBoolean()
        } else {
            false
        }


        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        /*
            TODO should \ be handled specially?
            In any case, would have same handling as AnyCharacterRxGene
         */
        return if (!flags.isCaseable(value)) {
            value.toString()
        }
        else if (useUpperCase) {
            value.uppercaseChar().toString()
        }
        else {
            value.lowercaseChar().toString()
        }
    }


    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is CharacterRangeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return getValueAsPrintableString(targetFormat = null) ==
                other.getValueAsPrintableString(targetFormat = null)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {

        val gene = other.getPhenotype()

        if(gene is CharacterRangeRxGene){
            value = gene.value
            useUpperCase = gene.useUpperCase
            return true
        }
        LoggingUtil.uniqueWarn(log,"cannot bind CharacterClassEscapeRxGene with ${gene::class.java.simpleName}")

        return false
    }
}
