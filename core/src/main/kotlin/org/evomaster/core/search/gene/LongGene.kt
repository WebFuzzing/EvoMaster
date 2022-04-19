package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class LongGene(
        name: String,
        value: Long = 0,
        /** Inclusive */
        min : Long? = null,
        /** Inclusive */
        max : Long? = null
) : NumberGene<Long>(name, value, min, max) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(LongGene::class.java)
    }

    override fun copyContent(): Gene {
        val copy = LongGene(name, value, min, max)
        return copy
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        value = NumberMutator.randomizeLong(value, min, max, randomness, forceNewValue)
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{
        val mutated = super.mutate(randomness, apc, mwc, allGenes, selectionStrategy, enableAdaptiveGeneMutation, additionalGeneMutationInfo)
        if (mutated) return true

        value = NumberMutator.mutateLong(value, min, max, randomness, apc)

        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return value.toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is LongGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is LongGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

    override fun innerGene(): List<Gene> = listOf()

    override fun bindValueBasedOn(gene: Gene): Boolean {
        when(gene){
            is LongGene -> value = gene.value
            is FloatGene -> value = gene.value.toLong()
            is IntegerGene -> value = gene.value.toLong()
            is DoubleGene -> value = gene.value.toLong()
            is StringGene -> {
                value = gene.value.toLongOrNull() ?: return false
            }
            is Base64StringGene ->{
                value = gene.data.value.toLongOrNull() ?: return false
            }
            is ImmutableDataHolderGene -> {
                value = gene.value.toLongOrNull() ?: return false
            }
            is SqlPrimaryKeyGene ->{
                value = gene.uniqueId
            }
            else -> {
                log.info("Do not support to bind long gene with the type: ${gene::class.java.simpleName}")
                return false
            }
        }
        return true
    }


    override fun compareTo(other: ComparableGene): Int {
        if (other !is LongGene) {
            throw RuntimeException("Expected LongGene. Cannot compare to ${other::javaClass} instance")
        }
        return this.toLong().compareTo(other.toLong())
    }

    override fun getMaximum(): Long = max?: Long.MAX_VALUE

    override fun getMinimum(): Long = min?: Long.MIN_VALUE
}