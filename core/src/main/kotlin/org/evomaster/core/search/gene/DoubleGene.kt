package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.DifferentGeneInHistory
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.evomaster.core.utils.NumberCalculationUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class DoubleGene(name: String,
                 value: Double = 0.0,
                 min: Double? = null,
                 max: Double? = null,
                 /**
                  * specified precision
                  */
                 precision: Int? = null,
                 /**
                  * specified scale
                  */
                 scale: Int? = null
) : FloatingPointNumber<Double>(name, value,
    min = if (precision != null && scale != null && min == null) NumberCalculationUtil.boundaryDecimal(precision, scale).first else min,
    max = if (precision != null && scale != null && max == null) NumberCalculationUtil.boundaryDecimal(precision, scale).second else max,
    precision, scale) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(DoubleGene::class.java)
    }

    override fun copyContent() = DoubleGene(name, value, min, max, precision, scale)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        value = NumberMutator.randomizeDouble(min, max, scale, randomness)
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{

        val mutated = super.mutate(randomness, apc, mwc, allGenes, selectionStrategy, enableAdaptiveGeneMutation, additionalGeneMutationInfo)
        if (mutated) return true

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
            is BigDecimalGene -> value = try { gene.value.toDouble() } catch (e: Exception) { return false }
            is BigIntegerGene -> value = try { gene.value.toDouble() } catch (e: Exception) { return false }
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
            }
            is SeededGene<*> ->{
                return this.bindValueBasedOn(gene.getPhenotype())
            } else -> {
                LoggingUtil.uniqueWarn(log, "Do not support to bind double gene with the type: ${gene::class.java.simpleName}")
                return false
            }
        }
        return true
    }

    override fun getMaximum(): Double {
        return max?: Double.MAX_VALUE
    }

    override fun getMinimum(): Double {
        return min?: Double.MIN_VALUE
    }

    override fun compareTo(other: ComparableGene): Int {
        if (other !is DoubleGene) {
            throw RuntimeException("Expected DoubleGene. Cannot compare to ${other::javaClass} instance")
        }
        return this.toDouble().compareTo(other.toDouble())
    }

}