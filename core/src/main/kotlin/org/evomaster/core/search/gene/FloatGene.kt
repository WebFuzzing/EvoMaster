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
import org.evomaster.core.utils.CalculationUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow


class FloatGene(name: String,
                value: Float = 0.0f,
                /** Inclusive */
                min : Float? = null,
                /** Inclusive */
                max : Float? = null,
                /**
                 * specified precision
                 */
                val precision: Int? = null
) : NumberGene<Float>(name, value, min, max) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(FloatGene::class.java)
    }

    override fun copyContent() = FloatGene(name, value, min, max)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        var rand = randomness.nextFloat()
        if (isRangeSpecified() && ((rand < (min ?: Float.MIN_VALUE)) || (rand > (max ?: Float.MAX_VALUE)))){
            rand = randomness.nextDouble((min?:Float.MIN_VALUE).toDouble(), (max?:Float.MAX_VALUE).toDouble()).toFloat()
        }
        value = getFormattedValue(rand)

    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
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
                }catch (e: DifferentGeneInHistory){}
            }
        }

        /*
            TODO min/max for Float
            Man: update a bit by considering min and max,
            NEED a check by Andrea
         */
        var gaussianDelta = randomness.nextGaussian()
        if ((max != null && max == value && gaussianDelta > 0) || (min != null && min == value && gaussianDelta < 0) )
            gaussianDelta *= -1.0

        val maxRange = if (!isRangeSpecified()) Long.MAX_VALUE
        else if (gaussianDelta > 0)
            CalculationUtil.calculateIncrement(value.toDouble(), (max?: Float.MAX_VALUE).toDouble()).toLong()
        else
            CalculationUtil.calculateIncrement((min?: Float.MIN_VALUE).toDouble(), value.toDouble()).toLong()

        var res = modifyValue(randomness, value.toDouble(), delta = gaussianDelta, maxRange = maxRange, specifiedJumpDelta = GeneUtils.getDelta(randomness, apc, maxRange),precision == null).toFloat()

        if (precision != null && getFormattedValue() == getFormattedValue(res)){
            res += (if (gaussianDelta>0) 1.0f else -1.0f) * getMinimalDelta()!!
        }

        value = if (max != null && res > max) max
                else if (min != null && res < min) min
                else getFormattedValue(res)

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
            else -> {
                LoggingUtil.uniqueWarn(log, "Do not support to bind float gene with the type: ${gene::class.java.simpleName}")
                return false
            }
        }
        return true
    }

    override fun getFormattedValue(valueToFormat: Float?): Float {
        if (precision == null)
            return value
        return BigDecimal(value.toDouble()).setScale(precision, RoundingMode.HALF_UP).toFloat()
    }

    override fun getMinimalDelta(): Float? {
        if (precision == null) return null
        val num = (10.0).pow(precision)
        return BigDecimal(1.0/num).setScale(precision, RoundingMode.HALF_UP).toFloat()
    }
}