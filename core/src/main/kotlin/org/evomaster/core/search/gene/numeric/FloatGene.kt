package org.evomaster.core.search.gene.numeric

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.interfaces.ComparableGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.string.Base64StringGene
import org.evomaster.core.search.gene.string.NumericStringGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.utils.NumberMutatorUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.evomaster.core.utils.NumberCalculationUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
) : FloatingPointNumberGene<Float>(name, value,
        min = NumberMutatorUtils.handleMinMaxInConstructor(
            value = min,
            isMin = true,
            precision = precision,
            scale = scale,
            example = 0.0f
        ),
        max = NumberMutatorUtils.handleMinMaxInConstructor(
            value = max,
            isMin = false,
            precision = precision,
            scale = scale,
            example = 0.0f
        ),
        minInclusive, maxInclusive, precision, scale) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(FloatGene::class.java)
    }

    override fun copyContent() = FloatGene(name, value, min, max, minInclusive, maxInclusive, precision, scale)

    override fun setValueWithRawString(value: String) {
        this.value = value.toFloat()
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        val rand =
            NumberMutatorUtils.randomizeDouble(getMinimum().toDouble(), getMaximum().toDouble(), scale, randomness)
        value = getFormattedValue(rand.toFloat())
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        if(enableAdaptiveGeneMutation){
            val mutated = applyHistoryBasedMutation(this, additionalGeneMutationInfo!!)
            if (mutated) return true
        }

        value = mutateFloatingPointNumber(randomness, apc).run {
            // it is weird, value sometimes becomes Double with genrric function
            getFormattedValue(this.toFloat())
        }

        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return getFormattedValue().toString()
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is FloatGene) {
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
        if (other !is FloatGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
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
                return this.setValueBasedOn(gene.getPhenotype() as Gene)
            }
            is NumericStringGene ->{
                return this.setValueBasedOn(gene.number)
            }
            else -> {
                LoggingUtil.uniqueWarn(
                    log,
                    "Do not support to bind float gene with the type: ${gene::class.java.simpleName}"
                )
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

    override fun setValueBasedOn(value: String) : Boolean{

        try{
            this.value = value.toFloat()
        }catch (e: NumberFormatException){
            return false
        }

        return true
    }
}
