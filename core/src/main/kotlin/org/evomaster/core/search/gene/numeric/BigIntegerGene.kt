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
import java.math.BigDecimal
import java.math.BigInteger

/**
 * gene represents bigInteger
 *
 * note that
 * currently the mutation of biginteger is limited to long mutation
 * however, biginteger could be mutated with `signum` and `mag`
 */
class BigIntegerGene(
    name: String,
    value: BigInteger? = null,
    min : BigInteger? = null,
    max : BigInteger? = null,
    precision : Int? = null,
    minInclusive : Boolean = true,
    maxInclusive : Boolean = true
) : IntegralNumberGene<BigInteger>(name, value,
    min = deriveMin(precision, min),
    max = deriveMax(precision, max),
    precision, minInclusive, maxInclusive) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(BigIntegerGene::class.java)
        private const val MAX_INTERNAL_PRECISION = 19

        private fun deriveMin(precision: Int?, min: BigInteger?) : BigInteger?{
            validatePrecision(precision)
            if (precision == MAX_INTERNAL_PRECISION)
                return Long.MIN_VALUE.toBigInteger()

            return if (precision != null ) (-NumberCalculationUtil.upperBound(precision, 0)).toBigInteger().run { if (min== null || this > min) this else min } else min
        }

        private fun deriveMax(precision: Int?, max: BigInteger?) : BigInteger?{
            validatePrecision(precision)
            if (precision == MAX_INTERNAL_PRECISION)
                return Long.MAX_VALUE.toBigInteger()

            return if (precision != null ) NumberCalculationUtil.upperBound(precision, 0).toBigInteger().run { if (max == null || this < max) this else max } else max
        }

        private fun validatePrecision(precision: Int?){
            if (precision != null && (precision > MAX_INTERNAL_PRECISION || precision <= 0))
                throw IllegalArgumentException("invalid precision: the precision for BigInteger are $MAX_INTERNAL_PRECISION for max and 1 for min, but $precision is specified")
        }
    }

    init {

        if (getMaximum() == getMinimum())
            this.value = getMinimum()

    }

    override fun copyContent(): BigIntegerGene =
        BigIntegerGene(name, value, min, max, precision, minInclusive, maxInclusive)


    override fun compareTo(other: ComparableGene): Int {
        if (other !is BigIntegerGene) {
            throw RuntimeException("Expected BigIntegerGene. Cannot compare to ${other::javaClass} instance")
        }
        return value.compareTo(other.value)
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        val longValue = NumberMutatorUtils.randomizeLong(
            value.toLong(),
            getMinUsedInSearch(),
            getMaxUsedInSearch(),
            randomness,
            tryToForceNewValue
        )
        setValueWithLong(longValue)
    }

    override fun shallowMutate(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        mwc: MutationWeightControl,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        if(enableAdaptiveGeneMutation){
            val mutated = applyHistoryBasedMutation(this, additionalGeneMutationInfo!!)
            if (mutated) return true
        }

        val longValue =
            NumberMutatorUtils.mutateLong(value.toLong(), getMinUsedInSearch(), getMaxUsedInSearch(), randomness, apc)
        setValueWithLong(longValue)
        return true
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        // note that it could return scientific representation
        return value.toString()
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is BigIntegerGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        //BigInteger is immutable, just refer to the value of other gene
        val current = this.value
        this.value = other.value
        if (!isLocallyValid()){
            this.value = current
            return false
        }

        return true
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is BigIntegerGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value.compareTo(other.value) == 0
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        when(gene){
            is SeededGene<*> -> return this.setValueBasedOn(gene.getPhenotype() as Gene)
            is NumericStringGene -> return this.setValueBasedOn(gene.number)
            is LongGene -> setValueWithLong(gene.value)
            is FloatGene -> setValueWithLong(gene.value.toLong())
            is IntegerGene -> setValueWithLong(gene.value.toLong())
            is DoubleGene -> setValueWithLong(gene.value.toLong())
            /*
                Man: TODO, it might need to handle radix for string, base64String
             */
            is StringGene -> gene.value.toBigIntegerOrNull()?: return false
            is Base64StringGene -> gene.data.value.toBigIntegerOrNull()?: return false
            is ImmutableDataHolderGene -> gene.value.toBigIntegerOrNull()?: return false
            is SqlPrimaryKeyGene -> setValueWithLong(gene.uniqueId)
            is BigIntegerGene -> value = gene.value
            is BigDecimalGene -> setValueWithDecimal(gene.value)
            else -> {
                log.info("Do not support to bind long gene with the type: ${gene::class.java.simpleName}")
                return false
            }
        }

        return true
    }

    fun setValueWithDecimal(decimal: BigDecimal){
        value = decimal.toBigInteger()
    }

    override fun setValueWithRawString(value: String) {
        setValueWithStringAndRadix(value, 10)
    }

    fun setValueWithStringAndRadix(str: String, radix: Int){
        value = BigInteger(str, radix)
    }

    fun setValueWithLong(longValue: Long) {
        value = BigInteger.valueOf(longValue)
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        if (max != null && value > max)
            return false
        if (min != null && value < min)
            return false
        return true
    }

    /**
     * note that for biginteger
     * if the value exceeds the range manipulated by search, we consider it immutable for the moment
     */
    override fun isMutable(): Boolean {
        return super.isMutable() && checkValueRangeInSearch()
    }

    private fun checkValueRangeInSearch() : Boolean{
        val r = value <= BigInteger.valueOf(Long.MAX_VALUE) &&
                value >= BigInteger.valueOf(Long.MIN_VALUE)
        if (!r)
            LoggingUtil.uniqueWarn(log, "value of BigInteger exceeds the range of mutation")
        return r
    }

    private fun getMinUsedInSearch() : Long {
        if (min!= null && min >= BigInteger.valueOf(Long.MAX_VALUE))
            throw IllegalStateException("not support yet: minimum value is greater than Long.MAX")

        val m = if (min == null || BigInteger.valueOf(Long.MIN_VALUE) >= min) Long.MIN_VALUE else min.toLong()
        return m.run { if (!minInclusive) this + 1L else this}
    }

    private fun getMaxUsedInSearch() : Long {
        if (max!= null && max <= BigInteger.valueOf(Long.MIN_VALUE))
            throw IllegalStateException("not support yet: max value is less than Long.MIN")

        val m = if (max == null || (min != null && BigInteger.valueOf(Long.MAX_VALUE) <= min)) Long.MAX_VALUE else max.toLong()
        return m.run { if (!maxInclusive) this - 1L else this }
    }

    override fun getMinimum(): BigInteger {
        return BigInteger.valueOf(getMinUsedInSearch())
    }

    override fun getMaximum(): BigInteger {
        return BigInteger.valueOf(getMaxUsedInSearch())
    }

    override fun getDefaultValue(): BigInteger {
        val df = getZero()
        if (df <= getMaximum() && df >= getMinimum())
            return df
        return NumberCalculationUtil.getMiddle(getMinimum(), getMaximum(), 0).toBigInteger()
    }

    override fun getZero(): BigInteger = BigInteger.ZERO

}