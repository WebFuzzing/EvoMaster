package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.NumericConstrains
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.DifferentGeneInHistory
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class DoubleGene(name: String,
                 value: Double = 0.0,
                 numericConstrains: NumericConstrains? = null,
                 /**
                  * specified precision
                  */
                 precision: Int? = null
) : FloatingPointNumber<Double>(name, value, numericConstrains , precision) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(DoubleGene::class.java)
    }

    private fun getRealMinimum(): Double {
        return if (numericConstrains?.getExclusiveMinimum() == true) {
            getMinimum() + getMinimalDelta() as Double
        } else getMinimum()
    }

    private fun getRealMaximum(): Double {
        return if (numericConstrains?.getExclusiveMaximum() == true) {
            getMaximum() - getMinimalDelta() as Double
        }  else getMaximum()
    }

    override fun copyContent() = DoubleGene(name, value, numericConstrains, precision)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        var rand = randomness.nextDouble()
        if (isRangeSpecified() && ((exceedsMin(rand)) || (exceedsMax(rand)))){
            rand = randomness.nextDouble(getRealMinimum(), getRealMaximum())
        }
        value = getFormattedValue(rand)

    }

    private fun exceedsMin(res: Double): Boolean {
        return if (numericConstrains?.getExclusiveMinimum() == true) {
            res <= getMinimum()
        } else {
            res < getMinimum()
        }
    }

    private fun exceedsMax(res: Double): Boolean {
        return if (numericConstrains?.getExclusiveMaximum() == true) {
            res >= getMaximum()
        } else {
            res > getMaximum()
        }
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl,
                        allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy,
                        enableAdaptiveGeneMutation: Boolean,
                        additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean {

        if (enableAdaptiveGeneMutation){
            additionalGeneMutationInfo?:throw IllegalArgumentException("additional gene mutation info should not be null when adaptive gene mutation is enabled")
            if (additionalGeneMutationInfo.hasHistory()){
                try {
                    additionalGeneMutationInfo.archiveGeneMutator.historyBasedValueMutation(
                            additionalGeneMutationInfo,
                            this,
                            allGenes
                    )
                    return true
                }catch (e : DifferentGeneInHistory){}

            }
        }

        value = mutateFloatingPointNumber(randomness, apc)

        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return getFormattedValue().toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is DoubleGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DoubleGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

    override fun innerGene(): List<Gene> = listOf()

    override fun bindValueBasedOn(gene: Gene) : Boolean{
        when(gene){
            is DoubleGene -> value = gene.value
            is FloatGene -> value = gene.value.toDouble()
            is IntegerGene -> value = gene.value.toDouble()
            is LongGene -> value = gene.value.toDouble()
            is StringGene -> {
                value = gene.value.toDoubleOrNull() ?: return false
            }
            is Base64StringGene ->{
                value = gene.data.value.toDoubleOrNull() ?: return false
            }
            is ImmutableDataHolderGene -> {
                value = gene.value.toDoubleOrNull() ?: return false
            }
            is SqlPrimaryKeyGene ->{
                value = gene.uniqueId.toDouble()
            } else -> {
                LoggingUtil.uniqueWarn(log, "Do not support to bind double gene with the type: ${gene::class.java.simpleName}")
                return false
            }
        }
        return true
    }

    override fun getMaximum(): Double {
        return max?.toDouble() ?: Double.MAX_VALUE
    }

    override fun getMinimum(): Double {
        return min?.toDouble() ?: Double.MIN_VALUE
    }

    override fun compareTo(other: ComparableGene): Int {
        if (other !is DoubleGene) {
            throw RuntimeException("Expected DoubleGene. Cannot compare to ${other::javaClass} instance")
        }
        return this.toDouble().compareTo(other.toDouble())
    }

}