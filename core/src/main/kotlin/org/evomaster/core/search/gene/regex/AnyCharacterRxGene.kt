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
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class AnyCharacterRxGene : RxAtom, SimpleGene("."){

    companion object{
        private val log : Logger = LoggerFactory.getLogger(AnyCharacterRxGene::class.java)
    }

    var value: Char = 'a'

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene {
        val copy = AnyCharacterRxGene()
        copy.value = this.value
        return copy
    }

    override fun setValueWithRawString(value: String) {
        // need to check
        val c = value.toCharArray().firstOrNull()
        if (c!= null)
            this.value = c
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        //TODO properly... this is just a tmp hack
        value = randomness.nextWordChar()
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

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is AnyCharacterRxGene) {
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
        if (other !is AnyCharacterRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        when(gene){
            is AnyCharacterRxGene -> {
                value = gene.value
            }
            is IntegerGene -> value = gene.value.toChar()
            is DoubleGene -> value = gene.value.toChar()
            is FloatGene -> value = gene.value.toChar()
            is LongGene -> value = gene.value.toChar()
            else -> {
                if (gene is StringGene && gene.value.length == 1)
                    value = gene.value.first()
                else if(gene is StringGene && gene.getSpecializationGene() != null){
                    return setValueBasedOn(gene.getSpecializationGene()!!)
                }else{
                    LoggingUtil.uniqueWarn(log, "cannot bind AnyCharacterRxGene with ${gene::class.java.simpleName}")
                    return false
                }
            }
        }
        return true
    }

}