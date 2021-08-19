package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class BooleanGene(
        name: String,
        var value: Boolean = true
) : Gene(name, mutableListOf()) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(BooleanGene::class.java)
    }

    override fun getChildren(): MutableList<Gene> = mutableListOf()

    override fun copyContent(): Gene {
        return BooleanGene(name, value)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        val k: Boolean = if (forceNewValue) {
            !value
        } else {
            randomness.nextBoolean()
        }

        value = k
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        value = ! value
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return value.toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is BooleanGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is BooleanGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

    override fun innerGene(): List<Gene> = listOf()

    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene !is BooleanGene){
            LoggingUtil.uniqueWarn(log, "Do not support to bind boolean gene with the type: ${gene::class.java.simpleName}")
            return false
        }
        value = gene.value
        return true
    }
}