package org.evomaster.core.search.gene.numeric

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

class LongGene(
        name: String,
        value: Long? = null,
        min : Long? = null,
        max : Long? = null,
        precision : Int? = null,
        minInclusive : Boolean = true,
        maxInclusive : Boolean = true
) : IntegralNumberGene<Long>(name, value,
    min = if (precision != null) (-NumberCalculationUtil.upperBound(precision, 0, maxValue = Long.MAX_VALUE)).toLong().run { if (min== null || this > min) this else min } else min,
    max = if (precision != null) NumberCalculationUtil.upperBound(precision, 0, maxValue = Long.MAX_VALUE).toLong().run { if (max == null || this < max) this else max } else max,
    precision, minInclusive, maxInclusive) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(LongGene::class.java)
    }

    init {
        if (getMaximum() == getMinimum())
            this.value = getMinimum()

    }

    override fun copyContent(): Gene {
        val copy = LongGene(name, value, min, max, precision, minInclusive, maxInclusive)
        return copy
    }

    override fun setValueWithRawString(value: String) {
        this.value = value.toLong()
    }


    override fun setValueBasedOn(value: String) : Boolean{

        try{
            this.value = value.toLong()
        }catch (e: NumberFormatException){
            return false
        }

        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        value = NumberMutatorUtils.randomizeLong(value, min, max, randomness, tryToForceNewValue)
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{
        if(enableAdaptiveGeneMutation){
            val mutated = applyHistoryBasedMutation(this, additionalGeneMutationInfo!!)
            if (mutated) return true
        }

        value = NumberMutatorUtils.mutateLong(value, min, max, randomness, apc)

        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        val stringValue = value.toString()
        return if(mode==GeneUtils.EscapeMode.EJSON) "{\"\$numberLong\":\"$stringValue\"}" else stringValue
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is LongGene) {
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
        if (other !is LongGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        when(gene){
            is LongGene -> value = gene.value
            is FloatGene -> value = gene.value.toLong()
            is IntegerGene -> value = gene.value.toLong()
            is DoubleGene -> value = gene.value.toLong()
            is BigDecimalGene -> value = try { gene.value.toLong() } catch (e: Exception) { return false }
            is BigIntegerGene -> value = try { gene.value.toLong() } catch (e: Exception) { return false }
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
            is SeededGene<*> ->{
                return this.setValueBasedOn(gene.getPhenotype() as Gene)
            }
            is NumericStringGene ->{
                return this.setValueBasedOn(gene.number)
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

    override fun getMaximum(): Long = (max?: Long.MAX_VALUE).run { if (!maxInclusive) this - 1L else this }

    override fun getMinimum(): Long = (min?: Long.MIN_VALUE).run { if (!minInclusive) this + 1L else this }

    override fun getDefaultValue(): Long {
        val df = getZero()
        if (df <= getMaximum() && df >= getMinimum())
            return df
        return NumberCalculationUtil.getMiddle(getMinimum(), getMaximum(), 0).toLong()
    }

    override fun getZero(): Long = 0L
}
