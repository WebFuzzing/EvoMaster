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
import org.evomaster.core.utils.NumberCalculationUtil.valueWithPrecision
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow


class DoubleGene(name: String,
                 value: Double = 0.0,
                 min: Double? = null,
                 max: Double? = null,
                 /**
                  * specified precision
                  */
                 val precision: Int? = null
) : NumberGene<Double>(name, value, min, max) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(DoubleGene::class.java)
    }

    override fun copyContent() = DoubleGene(name, value, min, max)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        var rand = randomness.nextDouble()
        if (isRangeSpecified() && ((rand < (min ?: Double.MIN_VALUE)) || (rand > (max ?: Double.MAX_VALUE)))){
            rand = randomness.nextDouble(min?:Double.MIN_VALUE, max?:Double.MAX_VALUE)
        }
        value = getFormattedValue(rand)

    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{

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

        /*
           TODO min/max for double
           Man: update a bit by considering min and max,
           NEED a check by Andrea
        */
        var gaussianDelta = randomness.nextGaussian()
        if (gaussianDelta == 0.0)
            gaussianDelta = randomness.nextGaussian()

        if ((max != null && max == value && gaussianDelta > 0) || (min != null && min == value && gaussianDelta < 0) )
            gaussianDelta *= -1.0

        val maxRange = if (!isRangeSpecified()) Long.MAX_VALUE
                    else if (gaussianDelta > 0)
                        NumberCalculationUtil.calculateIncrement(value, max?: Double.MAX_VALUE).toLong()
                    else
                        NumberCalculationUtil.calculateIncrement(min?: Double.MIN_VALUE, value).toLong()

        var res = modifyValue(randomness, value, delta = gaussianDelta, maxRange = maxRange, specifiedJumpDelta = GeneUtils.getDelta(randomness, apc, maxRange),precision == null)

        if (precision != null && getFormattedValue() == getFormattedValue(res)){
            res += (if (gaussianDelta>0) 1.0 else -1.0) * getMinimalDelta()!!
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

    override fun getFormattedValue(valueToFormat: Double?): Double {
        val fvalue = valueToFormat?:value
        if (precision == null)
            return fvalue
        return BigDecimal(fvalue).setScale(precision, RoundingMode.HALF_UP).toDouble()
    }

    override fun getMinimalDelta(): Double? {
        if (precision == null) return null
        return valueWithPrecision(1.0/((10.0).pow(precision)), precision).toDouble()
    }
}