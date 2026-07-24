package org.evomaster.core.search.gene.regex

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.evomaster.core.utils.CharacterRange
import org.evomaster.core.utils.MultiCharacterRange
import org.evomaster.core.utils.RegexFlags
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private const val DEFAULT_VALUE = 'a'
private const val ANY_CHARACTER_RX_GENE_DEFAULT_NAME = "."
private const val firstSurrogateChar = '\uD800'
private const val lastSurrogateChar = '\uDFFF'

/**
 * These are the characters that are considered line terminators by default (i.e.: no flags used). These are used here
 * as the `.` regex matches all characters but line terminators, unless `DOT_ALL` flag is enabled.
 */
private val defaultLineTerminators = listOf('\n', '\r', '\u0085', '\u2028', '\u2029').map{ CharacterRange(it) }
/**
 * When the `UNIX_LINES` flag is on, only `\n` is considered a line terminator. These are used here as the `.` regex matches
 * all characters but line terminators, unless `DOT_ALL` flag is enabled.
 */
private val unixLinesModeLineTerminators = listOf('\n').map{ CharacterRange(it) }

class AnyCharacterRxGene(
    val flags: RegexFlags = RegexFlags()
) : RxAtom, SimpleGene(ANY_CHARACTER_RX_GENE_DEFAULT_NAME) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(AnyCharacterRxGene::class.java)

        /** All characters except for line terminators are recognized by "." in regex, see:
         * https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#COMMENTS:~:text=range%20forming%20metacharacter.-,Line%20terminators,-A%20line%20terminator
         */
        /*
         TODO in Java regex lone surrogates match ".", but this causes trouble when appearing in a rest path,
            so for now we avoid surrogates altogether
         */
        val dotAllValidRanges = MultiCharacterRange(true,listOf(CharacterRange(firstSurrogateChar,lastSurrogateChar))) // all characters accepted
        val defaultValidRanges = MultiCharacterRange(true,defaultLineTerminators).intersect(dotAllValidRanges)
        val unixLinesValidRanges = MultiCharacterRange(true, unixLinesModeLineTerminators).intersect(dotAllValidRanges)
    }

    var value: Char = DEFAULT_VALUE // this default value is throwaway as randomize should be called before first usage

    val validRanges = when {
        flags.dotAll -> dotAllValidRanges
        flags.unixLines -> unixLinesValidRanges
        else -> defaultValidRanges
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene {
        val copy = AnyCharacterRxGene(flags)
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
        val previous = value

        do {
            value = validRanges.sample(randomness)
        } while (tryToForceNewValue && previous == value && validRanges.charCount > 1)
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        randomize(randomness, true)
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        /*
            TODO should \ be handled specially?
         */
        return value.toString()
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is AnyCharacterRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {

        val gene = other.getPhenotype()

        when(gene){
            is AnyCharacterRxGene -> { value = gene.value }
            is IntegerGene -> value = gene.value.toChar()
            is DoubleGene -> value = gene.value.toInt().toChar()
            is FloatGene -> value = gene.value.toInt().toChar()
            is LongGene -> value = gene.value.toInt().toChar()
            else -> {
                if (gene is StringGene && gene.value.length == 1) {
                    value = gene.value.first()
                } else{
                    LoggingUtil.uniqueWarn(log, "cannot bind AnyCharacterRxGene with ${gene::class.java.simpleName}")
                    return false
                }
            }
        }
        return true
    }

    /**
     * 1 if [value]'s first character is within [validRanges] (i.e. one `.` itself could
     * render, given the current flags), else 0.
     * @see [RxAbsorbable.absorbableCount]
     */
    override fun absorbableCount(value: String): Int {
        if (value.isEmpty()) {
            return 0
        }
        return if (validRanges.contains(value[0])) {
            1
        } else {
            0
        }
    }

    /** Always false: `.` always renders exactly one character.
     * @see [RxAbsorbable.canBeZeroWidth]
     */
    override val canBeZeroWidth: Boolean = false

    /**
     * Forces [value]'s first character onto this gene if `.` could render it; mirrors
     * [absorbableCount], so it never mutates when [absorbableCount] would return 0.
     * @see [RxAbsorbable.tryForce]
     */
    override fun tryForce(value: String): Int {
        require(value.isNotEmpty())
        val n = absorbableCount(value)
        if (n == 1) {
            this.value = value[0]
        }
        return n
    }
}
