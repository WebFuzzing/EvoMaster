package org.evomaster.core.search.gene.regex

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class AnyCharacterRxGene : RxAtom(".", listOf()){

    companion object{
        private val log : Logger = LoggerFactory.getLogger(AnyCharacterRxGene::class.java)
    }

    var value: Char = 'a'

    override fun getChildren(): List<Gene> = listOf()

    override fun copyContent(): Gene {
        val copy = AnyCharacterRxGene()
        copy.value = this.value
        return copy
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        //TODO properly... this is just a tmp hack
        value = randomness.nextWordChar()
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        randomize(randomness, true, allGenes)
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
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

    override fun innerGene(): List<Gene> = listOf()

    override fun bindValueBasedOn(gene: Gene): Boolean {
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
                    return bindValueBasedOn(gene.getSpecializationGene()!!)
                }else{
                    LoggingUtil.uniqueWarn(log, "cannot bind AnyCharacterRxGene with ${gene::class.java.simpleName}")
                    return false
                }
            }
        }
        return true
    }

}