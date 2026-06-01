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
import org.evomaster.core.utils.MultiCharacterRange
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class AnyCharacterRxGene : RxAtom, SimpleGene("."){

    companion object{
        private val log : Logger = LoggerFactory.getLogger(AnyCharacterRxGene::class.java)

        /** All characters except for line terminators are recognized by "." in regex, see:
         * https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#COMMENTS:~:text=range%20forming%20metacharacter.-,Line%20terminators,-A%20line%20terminator
         */
        val defaultValidRanges = MultiCharacterRange(true,"\n\r\u0085\u2028\u2029")
    }

    var value: Char = 'a'

    val validRanges = defaultValidRanges

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene {
        val copy = AnyCharacterRxGene()
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

        value = validRanges.sample(randomness)

        if (previous == value) {
            randomize(randomness, tryToForceNewValue)
        }
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

}
