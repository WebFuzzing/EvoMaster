package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class BooleanGene(
        name: String,
        var value: Boolean = true
) : SimpleGene(name) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(BooleanGene::class.java)
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }
    override fun copyContent(): Gene {
        return BooleanGene(name, value)
    }

    override fun setValueWithRawString(value: String) {
        this.value = value.toBoolean()
    }

    override fun setValueBasedOn(value: String) : Boolean{
        try{
            this.value = value.toBoolean()
            return true
        }catch (e: Exception){
            return false
        }
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        val k: Boolean = if (tryToForceNewValue) {
            !value
        } else {
            randomness.nextBoolean()
        }

        value = k
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        value = ! value
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return value.toString()
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is BooleanGene) {
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
        if (other !is BooleanGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        if (gene is SeededGene<*>){
            return this.setValueBasedOn(gene.getPhenotype()as Gene)
        }
        if (gene !is BooleanGene){
            LoggingUtil.uniqueWarn(log, "Do not support to bind boolean gene with the type: ${gene::class.java.simpleName}")
            return false
        }
        value = gene.value
        return true
    }
}