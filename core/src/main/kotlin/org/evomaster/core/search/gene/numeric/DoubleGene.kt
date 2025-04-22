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

class DoubleGene(name: String,
                 value: Double? = null,
                 min: Double? = null,
                 max: Double? = null,
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
) : FloatingPointNumberGene<Double>(name, value,
        min = NumberMutatorUtils.handleMinMaxInConstructor(
            value = min,
            isMin = true,
            precision = precision,
            scale = scale,
            example = 0.0
        ),
        max = NumberMutatorUtils.handleMinMaxInConstructor(
            value = max,
            isMin = false,
            precision = precision,
            scale = scale,
            example = 0.0
        ),
        minInclusive = minInclusive, maxInclusive = maxInclusive, precision = precision, scale = scale) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(DoubleGene::class.java)
    }

    override fun copyContent() = DoubleGene(name, value, min, max, minInclusive, maxInclusive, precision, scale)

    override fun setValueWithRawString(value: String) {
        try {
            this.value = value.toDouble()
        }catch (e: Exception){
            log.warn("cannot set double with $value")
        }
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        value = NumberMutatorUtils.randomizeDouble(getMinimum(), getMaximum(), scale, randomness)
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{

        if(enableAdaptiveGeneMutation){
            val mutated = applyHistoryBasedMutation(this, additionalGeneMutationInfo!!)
            if (mutated) return true
        }

        value = mutateFloatingPointNumber(randomness, apc)

        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        val stringValue = getFormattedValue().toString()
        return if(mode==GeneUtils.EscapeMode.EJSON) "{\"\$numberDouble\":\"$stringValue\"}" else stringValue
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is DoubleGene) {
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
        if (other !is DoubleGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }


    override fun setValueBasedOn(gene: Gene) : Boolean{
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
                return this.setValueBasedOn(gene.getPhenotype() as Gene)
            }
            is NumericStringGene ->{
                return this.setValueBasedOn(gene.number)
            }
            else -> {
                LoggingUtil.uniqueWarn(
                    log,
                    "Do not support to bind double gene with the type: ${gene::class.java.simpleName}"
                )
                return false
            }
        }
        return true
    }

    override fun getMinimum(): Double {
        if (minInclusive) return min?: -Double.MAX_VALUE
        val lowerBounder = if (min != null && min > -Double.MAX_VALUE) min + getMinimalDelta() else -NumberMutatorUtils.MAX_DOUBLE_EXCLUSIVE
        return getFormattedValue(lowerBounder)
    }

    override fun getMaximum(): Double {
        if (maxInclusive) return max?: Double.MAX_VALUE
        val upperBounder = if (max != null && max < Double.MAX_VALUE) max - getMinimalDelta() else NumberMutatorUtils.MAX_DOUBLE_EXCLUSIVE
        return getFormattedValue(upperBounder)
    }

    override fun compareTo(other: ComparableGene): Int {
        if (other !is DoubleGene) {
            throw RuntimeException("Expected DoubleGene. Cannot compare to ${other::javaClass} instance")
        }
        return this.toDouble().compareTo(other.toDouble())
    }

    override fun getDefaultValue(): Double {
        val df = super.getDefaultValue()
        if (df <= getMaximum() && df >= getMinimum())
            return df
        return NumberCalculationUtil.getMiddle(getMinimum(), getMaximum(), scale).toDouble()
    }

    /**
     * Set Double Gene from string value
     */
    override fun setValueBasedOn(value: String) : Boolean{

        try{
            this.value = value.toDouble()
        }catch (e: NumberFormatException){
            return false
        }
        return true
    }

    override fun getZero(): Double = 0.0
}