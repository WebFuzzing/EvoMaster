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
import java.math.RoundingMode


class FloatGene(name: String,
                value: Float? = null,
                min : Float? = null,
                max : Float? = null,
                minInclusive : Boolean = true,
                maxInclusive : Boolean = true,
                /**
                 * specified precision
                 */
                precision: Int? = null,
                /**
                 * specified scale
                 */
                scale: Int? = null
) : FloatingPointNumber<Float>(name, value,
    min = if (precision != null && scale != null) (-NumberCalculationUtil.upperBound(precision, scale)).toFloat().run { if (min== null || this > min) this else min } else min,
    max = if (precision != null && scale != null) NumberCalculationUtil.upperBound(precision, scale).toFloat().run { if (max == null || this < max) this else max } else max,
    minInclusive, maxInclusive, precision, scale) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(FloatGene::class.java)
    }

    override fun copyContent() = FloatGene(name, value, min, max, minInclusive, maxInclusive, precision, scale)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        val rand = NumberMutatorUtils.randomizeDouble(getMinimum().toDouble(), getMaximum().toDouble(), scale, randomness)
        value = getFormattedValue(rand.toFloat())
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        val mutated = super.mutate(randomness, apc, mwc, allGenes, selectionStrategy, enableAdaptiveGeneMutation, additionalGeneMutationInfo)
        if (mutated) return true

        value = mutateFloatingPointNumber(randomness, apc).run {
            // it is werid, value sometimes becomes Double with genric function
            getFormattedValue(this.toFloat())
        }

        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return getFormattedValue().toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is FloatGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is FloatGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }
    override fun innerGene(): List<Gene> = listOf()


    override fun bindValueBasedOn(gene: Gene): Boolean {
        when(gene){
            is FloatGene -> value = gene.value
            is DoubleGene -> value = gene.value.toFloat()
            is IntegerGene -> value = gene.value.toFloat()
            is LongGene -> value = gene.value.toFloat()
            is BigDecimalGene -> value = try { gene.value.toFloat() } catch (e: Exception) { return false }
            is BigIntegerGene -> value = try { gene.value.toFloat() } catch (e: Exception) { return false }
            is StringGene -> {
                value = gene.value.toFloatOrNull() ?: return false
            }
            is Base64StringGene ->{
                value = gene.data.value.toFloatOrNull() ?: return false
            }
            is ImmutableDataHolderGene -> {
                value = gene.value.toFloatOrNull() ?: return false
            }
            is SqlPrimaryKeyGene ->{
                value = gene.uniqueId.toFloat()
            }
            is SeededGene<*> ->{
                return this.bindValueBasedOn(gene.getPhenotype())
            }
            is NumericStringGene ->{
                return this.bindValueBasedOn(gene.number)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "Do not support to bind float gene with the type: ${gene::class.java.simpleName}")
                return false
            }
        }
        return true
    }

    override fun getMinimum(): Float {
        if (minInclusive) return min?: -Float.MAX_VALUE
        val lowerBounder = if (min != null && min > -Float.MAX_VALUE) min + getMinimalDelta() else -NumberMutatorUtils.MAX_FLOAT_EXCLUSIVE
        return getFormattedValue(lowerBounder)
    }

    override fun getMaximum(): Float {
        if (maxInclusive) return max?: Float.MAX_VALUE
        val upperBounder = if (max != null && max < Float.MAX_VALUE) max - getMinimalDelta() else NumberMutatorUtils.MAX_FLOAT_EXCLUSIVE
        return getFormattedValue(upperBounder)
    }

    override fun compareTo(other: ComparableGene): Int {
        if (other !is FloatGene) {
            throw ClassCastException("Cannot compare FloatGene to ${other::javaClass} instance")
        }
        return this.toFloat().compareTo(other.toFloat())
    }

    override fun getDefaultValue(): Float {
        val df = getZero()
        if (df <= getMaximum() && df >= getMinimum())
            return df
        return NumberCalculationUtil.getMiddle(getMinimum(), getMaximum(), scale).toFloat()
    }

    override fun getZero(): Float = 0.0f

}