package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.NumberMutator.mutateFloatingPointNumber
import org.evomaster.core.search.gene.NumberMutator.mutateLong
import org.evomaster.core.search.gene.NumberMutator.randomizeDouble
import org.evomaster.core.search.gene.NumberMutator.randomizeLong
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * gene representing BigDecimal
 *
 * note that
 * currently the mutation of bigdecimal is limited to mutations for long and double.
 * however, bigdecimal could be mutated with `unscaled value` (such as biginteger) and `scale`
 * if we further enable it, then [floatingPointMode] and [isFloatingPointMutable] are not needed
 */
class BigDecimalGene(
    name: String,
    value: BigDecimal = BigDecimal.ZERO,
    /** Inclusive */
    min : BigDecimal? = null,
    /** Inclusive */
    max : BigDecimal? = null,

    /**
     * indicate whether to employ float pointing to mutate the gene
     */
    var floatingPointMode : Boolean = true,

    /**
     * as jdk,
     * the number of decimal digits in this BigDecimal
     */
    val precision : Int?,

    /**
     * as jdk,
     * the number of digits to the right of the decimal point
     */
    val scale : Int?

) : NumberGene<BigDecimal>(name, value, min, max){

    companion object{
        private val log : Logger = LoggerFactory.getLogger(BigDecimalGene::class.java)

        /**
         * a low probability to change the mode, eg, from floating-point to long
         */
        private const val PROBABILITY_CHANGE_MODE = 0.05

        private val DEFAULT_ROUNDING_MODE : RoundingMode = RoundingMode.HALF_UP
    }

    init {
        if (precision != null && precision < 0)
            throw IllegalArgumentException("invalid precision: a negative number for the precision is not allowed")

        if (scale!= null){
            if (scale < 0)
                throw IllegalArgumentException("invalid scale: a negative number for the scale is not allowed")

            //if [scale] is specified, floating mode is immutable
            forbidFloatingPointModeMutable()
            floatingPointMode = scale > 0
        }
    }

    /**
     * whether to allow search to change [floatingPointMode]
     * */
    var isFloatingPointMutable : Boolean = true
        private set

    fun forbidFloatingPointModeMutable(){
        isFloatingPointMutable = false
    }

    override fun copyContent(): BigDecimalGene{
        val copy = BigDecimalGene(name, value, min, max, floatingPointMode, precision, scale)
        copy.isFloatingPointMutable = this.isFloatingPointMutable
        return copy
    }

    override fun compareTo(other: ComparableGene): Int {
        if (other !is BigDecimalGene) {
            throw RuntimeException("Expected BigIntegerGene. Cannot compare to ${other::javaClass} instance")
        }
        return value.compareTo(other.value)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        if (isFloatingPointMutable && randomness.nextBoolean()){
            floatingPointMode = randomness.nextBoolean()
        }

        if (floatingPointMode){
            val dValue = randomizeDouble(getMinUsedInSearch(), getMaxUsedInSearch(), scale, randomness)
            setValueWithDouble(dValue)
        }else{
            val longValue = randomizeLong(value.toLong(), getMinUsedInSearch().toLong(), getMaxUsedInSearch().toLong(), randomness, forceNewValue)
            setValueWithLong(longValue)
        }
    }

    override fun mutate(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        mwc: MutationWeightControl,
        allGenes: List<Gene>,
        selectionStrategy: SubsetGeneSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {

        if (isFloatingPointMutable && randomness.nextBoolean(PROBABILITY_CHANGE_MODE)){
            floatingPointMode = !floatingPointMode
        }

        // if the current value exceeds the range of long, then mode must be floating-point
        if (isFloatingPointMutable && !floatingPointMode && !withinLongRange())
            floatingPointMode = true

        val mutated = super.mutate(
            randomness,
            apc,
            mwc,
            allGenes,
            selectionStrategy,
            enableAdaptiveGeneMutation,
            additionalGeneMutationInfo
        )
        if (mutated) return true

        if (floatingPointMode){
            setValueWithDouble(mutateFloatingPointNumber(randomness, apc = apc, value = value.toDouble(), smin = getMinUsedInSearch(), smax = getMaxUsedInSearch(), scale=scale))
        }else{
            setValueWithLong(mutateLong(value = value.toLong(), min=getMinUsedInSearch().toLong(), max = getMaxUsedInSearch().toLong(), apc = apc, randomness = randomness))
        }
        return true
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return value.toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is BigDecimalGene)
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        // since bigdecimal is immutable, just refer to the value of other gene
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is BigDecimalGene)
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        return this.value.compareTo(other.value) == 0
    }

    override fun innerGene(): List<Gene> = listOf()

    override fun bindValueBasedOn(gene: Gene): Boolean {
        val bd = when(gene){
            is SeededGene<*> -> return this.bindValueBasedOn(gene.getPhenotype())
            is LongGene -> BigDecimal(gene.value)
            is FloatGene -> BigDecimal(gene.value.toDouble())
            is IntegerGene -> BigDecimal(gene.value)
            is DoubleGene -> BigDecimal(gene.value)
            is StringGene -> gene.value.toBigDecimalOrNull()?: return false
            is Base64StringGene -> gene.data.value.toBigDecimalOrNull()?: return false
            is ImmutableDataHolderGene -> gene.value.toBigDecimalOrNull()?: return false
            is SqlPrimaryKeyGene -> BigDecimal(gene.uniqueId)
            is BigIntegerGene -> BigDecimal(gene.value)
            else -> {
                log.info("Do not support to bind long gene with the type: ${gene::class.java.simpleName}")
                return false
            }
        }
        setValueWithDecimal(bd, precision, scale)
        return true
    }

    fun setValueWithDouble(doubleValue: Double){
        setValueWithDecimal(BigDecimal(doubleValue), precision, scale)
    }

    fun setValueWithLong(longValue: Long){
        setValueWithDecimal(BigDecimal(longValue), precision, scale)
    }

    private fun getRoundingMode() = DEFAULT_ROUNDING_MODE

    private fun setValueWithDecimal(bd: BigDecimal, precision: Int?, scale: Int?){
        value = if (precision == null){
            if (scale == null) bd
            else bd.setScale(scale, getRoundingMode())
        } else{
            val context = MathContext(precision, getRoundingMode())
            if (scale == null) bd.round(context)
            else BigDecimal(bd.unscaledValue(), scale, context)
        }
    }

    private fun getMinUsedInSearch() : Double {
        if (min == null || BigDecimal.valueOf(Double.MIN_VALUE) >= min) return Double.MIN_VALUE
        if (min >= BigDecimal.valueOf(Double.MAX_VALUE))
            throw IllegalStateException("not support yet: minimum value is greater than Double.MAX")
        return min.toDouble()
    }

    private fun getMaxUsedInSearch() : Double {
        if (max == null || BigDecimal.valueOf(Double.MAX_VALUE) <= min) return Double.MAX_VALUE
        if (max <= BigDecimal.valueOf(Double.MIN_VALUE))
            throw IllegalStateException("not support yet: max value is less than Double.MIN")
        return max.toDouble()
    }

    override fun getMinimum(): BigDecimal {
        return BigDecimal.valueOf(getMinUsedInSearch())
    }

    override fun getMaximum(): BigDecimal {
        return BigDecimal.valueOf(getMaxUsedInSearch())
    }

    override fun isValid(): Boolean {
        if (max != null && value <= max)
            return false
        if (min != null && value >= min)
            return false
        return true
    }

    /**
     * note that for bigdecimal
     * if the value exceeds the range manipulated by search, we consider it immutable for the moment
     */
    override fun isMutable(): Boolean {
        return super.isMutable() && checkValueRangeInSearch()
    }

    private fun checkValueRangeInSearch() : Boolean{

        val r = if (!isFloatingPointMutable && !floatingPointMode)
                    withinLongRange()
                else
                    value <= BigDecimal.valueOf(Double.MAX_VALUE) && value >= BigDecimal.valueOf(Double.MIN_VALUE)
        if (!r)
            LoggingUtil.uniqueWarn(log, "value of BigDecimal exceeds the range of mutation")
        return r
    }

    private fun withinLongRange() : Boolean = value <= BigDecimal.valueOf(Long.MAX_VALUE) && value >= BigDecimal.valueOf(Long.MIN_VALUE)
}